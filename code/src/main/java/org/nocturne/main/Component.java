/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.template.Configuration;
import freemarker.template.Template;
import net.sf.cglib.reflect.FastMethod;
import org.apache.log4j.Logger;
import org.nocturne.caption.CaptionDirective;
import org.nocturne.exception.*;
import org.nocturne.link.LinkDirective;
import org.nocturne.link.Links;
import org.nocturne.util.ReflectionUtil;
import org.nocturne.util.RequestUtil;
import org.nocturne.validation.ValidationException;
import org.nocturne.validation.Validator;
import org.nocturne.cache.CacheHandler;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Base class for Page and Frame (controllers). You should not use
 * Component directly (creates subclasses of it).
 *
 * @author Mike Mirzayanov
 */
public abstract class Component {
    /**
     * Has been initialized?
     */
    private boolean initialized;

    /**
     * Default is null, which means no caching.
     */
    private CacheHandler cacheHandler;

    /**
     * Gson instance - needed in the debug mode to store JSON-ized objects in session
     * instead of real objects, because their classes between reloads can change.
     */
    private final Gson gson = new Gson();

    /**
     * Log4j logger.
     */
    private Logger logger;

    /**
     * Freemarker configuration.
     */
    private Configuration templateEngineConfiguration;

    /**
     * Freemarker template.
     */
    private Template template = null;

    /**
     * Map to store template variables.
     */
    private Map<String, Object> templateMap = new HashMap<String, Object>();

    /**
     * Map to store frame contents after parse().
     */
    private Map<String, String> frameMap = new HashMap<String, String>();

    /**
     * Should workflow skip template processing?
     */
    private boolean skipTemplate;

    /**
     * Http filter config.
     */
    private FilterConfig filterConfig;

    /**
     * Http servlet request.
     */
    private HttpServletRequest request;

    /**
     * Http servlet response.
     */
    private HttpServletResponse response;

    /**
     * List of validators by parameter names.
     */
    private Map<String, List<Validator>> validators;

    /**
     * Http servlet response output stream.
     */
    private OutputStream outputStream;

    /**
     * Object to inject parameters from request.
     */
    private ParametersInjector parametersInjector = new ParametersInjector(this);

    /**
     * Parent component (needed for frames).
     */
    private Component parentComponent;

    /**
     * Http servlet response writer.
     */
    private PrintWriter writer = null;

    /**
     * Stores cached instances for #getInstance(clazz, index) method.
     */
    private Map<Class<?>, List<Object>> cacheForGetInstance
            = new HashMap<Class<?>, List<Object>>();

    /**
     * Stores current indecies of instances for #getInstance(clazz).
     */
    private Map<Class<?>, Integer> instanceIndexForCacheForGetInstance =
            new HashMap<Class<?>, Integer>();

    /**
     * Stores information about action, validation and invalid methods for component.
     */
    private static Map<Class<? extends Component>, ActionMap> actionMaps
            = new Hashtable<Class<? extends Component>, ActionMap>();

    /**
     * Template file name: simple class name + ".ftl" by default.
     */
    private String templateFileName = getClass().getSimpleName() + ".ftl";

    /**
     * Map, containing parameters, which will be checked before request.getParameter().
     */
    private Map<String, String> overrideParameters = new HashMap<String, String>();

    /**
     * Stores params from request.
     */
    private Map<String, String> requestParams = new HashMap<String, String>();

    /**
     * @return Component cache handler. Use it if you want to avoid typical
     *         rendering life-cycle and get component HTML (or other parsed result) from
     *         cache.
     */
    public CacheHandler getCacheHandler() {
        return cacheHandler;
    }

    /**
     * @param cacheHandler Component cache handler. Use it if you want to avoid typical
     *                     rendering life-cycle and get component HTML (or other parsed result) from
     *                     cache.
     */
    public void setCacheHandler(CacheHandler cacheHandler) {
        this.cacheHandler = cacheHandler;
    }

    /**
     * @return Action name or empty string if not specified.
     */
    protected String getActionName() {
        return ApplicationContext.getInstance().getRequestAction();
    }

    /**
     * This is internal nocturne method. Do not call it.
     *
     * @param actionParameter Action name.
     */
    protected void internalRunAction(String actionParameter) {
        try {
            ActionMap actionMap = actionMaps.get(getClass());
            FastMethod validateMethod = actionMap.getValidateMethod(actionParameter);
            Boolean validationResult = true;
            if (validateMethod != null) {
                validationResult = (Boolean) validateMethod.invoke(this, new Object[0]);
            }

            if (validationResult) {
                FastMethod actionMethod = actionMap.getActionMethod(actionParameter);
                if (actionMethod != null) {
                    actionMethod.invoke(this, new Object[0]);
                } else {
                    throw new NocturneException("Can't find action method for component "
                            + getClass().getName() + " and action parameter = " + actionParameter + ".");
                }
            } else {
                FastMethod invalidMethod = actionMap.getInvalidMethod(actionParameter);
                if (invalidMethod != null) {
                    invalidMethod.invoke(this, new Object[0]);
                }
            }
        } catch (InvocationTargetException e) {
            if (!(e.getCause() instanceof AbortException)) {
                throw new NocturneException("Can't invoke validate or action method for component class "
                        + getClass().getName() + " [action=" + actionParameter + "].", e);
            } else {
                throw (AbortException) e.getCause();
            }
        }
    }

    /**
     * @param templateFileName By default template file name equals to
     *                         simple class name + ".ftl". You can change it via
     *                         the method. You may change it once in init() method or
     *                         on each request. But name will not be changed to
     *                         default value between requests.
     */
    public void setTemplateFileName(String templateFileName) {
        this.templateFileName = templateFileName;
    }

    /**
     * @return Http servlet response output stream. It will call getResponse().getOutputStream()
     *         but exactly once. It uses internal field to store value.
     */
    public OutputStream getOutputStream() {
        if (outputStream == null) {
            try {
                outputStream = getResponse().getOutputStream();
            } catch (IOException e) {
                throw new ServletException("Can't get response output stream.", e);
            }
        }

        return outputStream;
    }

    /**
     * @return Http servlet response writer. Uses UTF-8 encoding. Invokes
     *         new PrintWriter(new OutputStreamWriter(getOutputStream(), "UTF-8"), true) but
     *         exactly once (uses lazy calculations).
     */
    public PrintWriter getWriter() {
        if (writer == null) {
            try {
                writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), "UTF-8"), true);
            } catch (UnsupportedEncodingException e) {
                throw new ConfigurationException("Can't set encoding for writer.", e);
            }
        }
        return writer;
    }

    /**
     * @param css Path to CSS resource. Use this method to add specific to
     *            the controller CSS resource. You can use following code in your FTL-file:
     *            {@code
     *            <#list css as file>
     *            <link rel="stylesheet" type="text/css" href="${home}css/${file}" charset="utf-8">
     *            </#list>
     *            }
     */
    public void addCss(String css) {
        getCurrentPage().getCssSet().add(css);
    }

    /**
     * @param js Path to JS resource. Use this method to add specific to
     *           the controller JS resource. You can use following code in your FTL-file:
     *           {@code
     *           <#list js as file>
     *           <link rel="stylesheet" type="text/js" href="${home}js/${file}" charset="utf-8">
     *           </#list>
     *           }
     */
    public void addJs(String js) {
        getCurrentPage().getJsSet().add(js);
    }

    /**
     * @return Returns log4j logger for this controller. So you don't need
     *         to add field Logger logger = Logger.getLogger(MyPage.class) and use
     *         getLogger().
     */
    public Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(this.getClass());
        }

        return logger;
    }

    /**
     * @param key Session variable name.
     * @return returns true iff session contains attrubute with name "key".
     */
    public boolean hasSession(String key) {
        try {
            HttpSession session = request.getSession(false);
            return session != null && session.getAttribute(key) != null;
        } catch (IllegalStateException e) {
            throw new SessionInvalidatedException();
        }
    }

    private void internalPutSession(String key, Object value) {
        getCurrentPage().putRequestCache(key, value);

        if (ApplicationContext.getInstance().isDebug()) {
            String json = gson.toJson(value);
            request.getSession().setAttribute(key, json);
        } else {
            request.getSession().setAttribute(key, value);
        }
    }

    /**
     * Put into the session attribute with name "key" and value "value".
     *
     * @param key   Session attribute name.
     * @param value Session attribute value.
     */
    public void putSession(String key, Object value) {
        try {
            internalPutSession(key, value);
        } catch (IllegalStateException e) {
            throw new SessionInvalidatedException();
        }
    }

    /**
     * @param key The name of session attribute to delete.
     */
    public void removeSession(String key) {
        try {
            getCurrentPage().removeRequestCache(key);
            request.getSession().removeAttribute(key);
        } catch (IllegalStateException e) {
            throw new SessionInvalidatedException();
        }
    }

    /**
     * @param key   Session attribute name.
     * @param clazz Value class.
     * @return Value as instance of class "clazz". Use JSON as internal format to
     *         store values in the debug mode.
     */
    @SuppressWarnings({"unchecked"})
    public <T> T getSession(String key, Class<T> clazz) {
        try {
            Object fromRequestCache = getCurrentPage().getRequestCache(key);

            if (fromRequestCache != null) {
                return (T) fromRequestCache;
            }

            T result;

            HttpSession session = request.getSession(false);

            if (ApplicationContext.getInstance().isDebug()) {
                String json = null;

                if (session != null) {
                    json = (String) session.getAttribute(key);
                }

                if (json != null) {
                    result = gson.fromJson(json, clazz);
                } else {
                    result = null;
                }
            } else {
                try {
                    if (session == null) {
                        result = null;
                    } else {
                        result = (T) session.getAttribute(key);
                    }
                } catch (Exception e) {
                    result = null;
                }
            }

            getCurrentPage().putRequestCache(key, result);

            return result;
        } catch (IllegalStateException e) {
            throw new SessionInvalidatedException();
        }
    }

    /**
     * @param key  Session attribute name.
     * @param type Value type.
     * @return Value as instance of type "type". Use JSON as internal format to
     *         store values in the debug mode.
     */
    @SuppressWarnings({"unchecked", "RedundantTypeArguments"})
    public <T> T getSession(String key, Type type) {
        try {
            Object fromRequestCache = getCurrentPage().getRequestCache(key);

            if (fromRequestCache != null) {
                return (T) fromRequestCache;
            }

            T result;
            HttpSession session = request.getSession(false);

            if (ApplicationContext.getInstance().isDebug()) {
                String json = null;

                if (session != null) {
                    json = (String) session.getAttribute(key);
                }

                if (json != null) {
                    result = gson.<T>fromJson(json, type);
                } else {
                    result = null;
                }
            } else {
                if (session == null) {
                    result = null;
                } else {
                    result = (T) session.getAttribute(key);
                }
            }

            getCurrentPage().putRequestCache(key, result);
            return result;
        } catch (IllegalStateException e) {
            throw new SessionInvalidatedException();
        }
    }

    void addOverrideParameter(String name, String value) {
        overrideParameters.put(name, value);
    }

    /**
     * @return Http filter config.
     */
    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    public Configuration getTemplateEngineConfiguration() {
        return templateEngineConfiguration;
    }

    void setTemplateEngineConfiguration(Configuration templateEngineConfiguration) {
        this.templateEngineConfiguration = templateEngineConfiguration;
    }

    /**
     * @return Creates (if needed) and returns templat. Uses simple class name as template
     *         name (+ ".ftl") or specified if setTemplateFile(String) has been called. Do
     *         not call the method getTemplate() manually.
     */
    public Template getTemplate() {
        if (!skipTemplate) {
            if (template == null || !template.getName().equals(templateFileName)) {
                try {
                    template = getTemplateEngineConfiguration().getTemplate(templateFileName, ApplicationContext.getInstance().getLocale());
                } catch (IOException e) {
                    throw new FreemarkerException("Can't get freemarker template [name=" + templateFileName + "].", e);
                }
            }
        } else {
            template = null;
        }
        //ApplicationContext.getInstance().setComponentByTemplate(template, this);
        return template;
    }

    private void ensureArgumentIs(String key, Object value, String expectedKey, Object expectedValue) {
        if (key.equals(expectedKey) && !expectedValue.equals(value)) {
            throw new IllegalArgumentException("Argument '" + key + "' is reserved for internal usage.");
        }
    }

    /**
     * @param key Template variable name to be removed.
     */
    public void remove(String key) {
        internalGetTemplateMap().remove(key);
    }

    /**
     * Adds variable to template variables map. Template variables map
     * is local between different controllers. So if you add variable into
     * a page, it will not add variable into its internal frame variables map
     * (and vice versa).
     *
     * @param key   Variable name.
     * @param value Variable value.
     */
    public void put(String key, Object value) {
        validateParameter(key, value);

        internalGetTemplateMap().put(key, value);
    }

    /**
     * Puts variable into global template variables map.
     * Global template variables map is owned by current page.
     *
     * @param key   Variable name.
     * @param value Variable value.
     */
    public void putGlobal(String key, Object value) {
        validateParameter(key, value);
        getCurrentPage().internalGetGlobalTemplateMap().put(key, value);
    }

    private void validateParameter(String key, Object value) {
        ensureArgumentIs(key, value, "frame", FrameDirective.getInstance());
        ensureArgumentIs(key, value, "css", getCurrentPage().getCssSet());
        ensureArgumentIs(key, value, "js", getCurrentPage().getJsSet());
    }

    /**
     * Inverse operation to put(String, Object). Doesn't get from parameters, but
     * gets from the template variables map!
     *
     * @param key Template variable name.
     * @return Template variable value or {@code null} if not found.
     */
    public Object get(String key) {
        return internalGetTemplateMap().get(key);
    }

//    protected void setTemplateName(String name) {
//        try {
//            template = getTemplateEngineConfiguration().getTemplate(
//                    name,
//                    ApplicationContext.getInstance().getLocale()
//            );
//        } catch (IOException e) {
//            throw new FreemarkerException("Can't get freemarker template [name=" + name + "].", e);
//        }
//    }

    /**
     * @return Is template processing will be skipped? It may be usefull for AJAX pages.
     */
    boolean isSkipTemplate() {
        return skipTemplate;
    }

    /**
     * Call it if you don't want current component will
     * parse templates. It may be usefull for AJAX pages.
     */
    protected void skipTemplate() {
        skipTemplate = true;
    }

    /**
     * Call it to rollback skipTemplate().
     */
    protected void unskipTemplate() {
        skipTemplate = false;
    }

    Map<String, Object> internalGetTemplateMap() {
        return templateMap;
    }

    public Map<String, Object> getTemplateMap() {
        return new HashMap<String, Object>(templateMap);
    }

    /**
     * Adds debug entry into log.
     *
     * @param format Message possibly containing %s and so on.
     * @param args   Parameters for format.
     */
    protected void debug(String format, Object... args) {
        getLogger().debug(String.format(format, args));
    }

    /**
     * Adds info entry into log.
     *
     * @param format Message possibly containing %s and so on.
     * @param args   Parameters for format.
     */
    protected void info(String format, Object... args) {
        getLogger().info(String.format(format, args));
    }

    /**
     * Adds warning entry into log.
     *
     * @param format Message possibly containing %s and so on.
     * @param args   Parameters for format.
     */
    protected void warn(String format, Object... args) {
        getLogger().warn(String.format(format, args));
    }

    /**
     * Adds error entry into log.
     *
     * @param format Message possibly containing %s and so on.
     * @param args   Parameters for format.
     */
    protected void error(String format, Object... args) {
        getLogger().error(String.format(format, args));
    }

    /**
     * Adds fatal entry into log.
     *
     * @param format Message possibly containing %s and so on.
     * @param args   Parameters for format.
     */
    protected void fatal(String format, Object... args) {
        getLogger().fatal(String.format(format, args));
    }

    /**
     * @param key Parameter name.
     * @return Gets parameter as string.
     */
    public String getString(String key) {
        if (overrideParameters.containsKey(key)) {
            return overrideParameters.get(key);
        } else {
            return requestParams.get(key);
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as boolean. Returns {@code false}
     *         on invalid boolean value. Returns {@code true} if
     *         key is "true" or "on" (case ignored).
     */
    public boolean getBoolean(String key) {
        return "true".equalsIgnoreCase(key) || "on".equalsIgnoreCase(key);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as integer. Returns {@code 0}
     *         on invalid integer value.
     */
    public int getInteger(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as long. Returns {@code 0}
     *         on invalid long value.
     */
    public long getLong(String key) {
        try {
            return Long.parseLong(getString(key));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as double. Returns {@code 0.0}
     *         on invalid double value.
     */
    public double getDouble(String key) {
        try {
            return Double.parseDouble(getString(key));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as double. Returns {@code 0.0}
     *         on invalid float value.
     */
    public double getFloat(String key) {
        try {
            return Float.parseFloat(getString(key));
        } catch (NumberFormatException e) {
            return 0.0F;
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns {@code true} if there is such parameter and
     *         it differs from {@code null}.
     */
    protected boolean hasParameter(String key) {
        return getString(key) != null;
    }

    Map<String, String> getRequestParams() {
        return new HashMap<String, String>(requestParams);
    }

    void setRequest(HttpServletRequest request) {
        this.request = request;
        setupRequestParams();
    }

    private void setupRequestParams() {
        if (this instanceof Page) {
            setupRequestParamsForPage();
        } else {
            if (this instanceof Frame) {
                requestParams = ApplicationContext.getInstance().getCurrentPage().getRequestParams();
            } else {
                throw new NocturneException("Expected page or frame class.");
            }
        }
    }

    private void setupRequestParamsForPage() {
        requestParams = RequestUtil.getRequestParams(request);
    }

    void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    /**
     * @return Http servlet request.
     */
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return Http servlet response.
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * Override it to initialize component instance.
     * The method will be called exactly once for each instance.
     */
    public void init() {
        // No operations.
    }

    /**
     * Override it to initialize component instance before action.
     */
    public void initializeAction() {
        // No operations.
    }

    /**
     * Override it to define default action handler.
     * Use @Action to specify other method as default.
     */
    public abstract void action();

    /**
     * Override it to initialize component instance after action.
     */
    public void finalizeAction() {
        // No operations.
    }

    void setup(Frame frame) {
        frame.setRequest(getRequest());
        frame.setResponse(getResponse());
        frame.setFilterConfig(getFilterConfig());
        frame.setTemplateEngineConfiguration(getTemplateEngineConfiguration());
    }

    String getFrameHtml(String key) {
        return frameMap.get(key);
    }

    /**
     * Parses frame and stores its raw HTML (or some other parsing result, possibly, JSON or some other)
     * in special internal map.
     * You can insert parsed frame using directive {@code <@frame name="key"/>}.
     *
     * @param key   Parsed frame name, refer to it via name attribute for
     *              directive @frame.
     * @param frame Frame instance.
     */
    public void parse(String key, Frame frame) {
        setup(frame);
        frameMap.put(key, frame.parseTemplate());
    }

    /**
     * Sets frame body without #parse().
     *
     * @param key       Frame key (name).
     * @param frameBody Parsed frame body.
     */
    public void setFrameBody(String key, String frameBody) {
        frameMap.put(key, frameBody);
    }

    /**
     * Parses frame and returns raw HTML
     * (or some other parsing result, possibly, JSON or some other).
     *
     * @param frame Frame to be parsed.
     * @return Frame raw HTML (or some other parsing result, possibly, JSON or some other).
     */
    public String parse(Frame frame) {
        setup(frame);
        return frame.parseTemplate();
    }

    void prepareForAction() {
        initializeIfNeeded();

        parentComponent = ApplicationContext.getInstance().getCurrentComponent();
        ApplicationContext.getInstance().setCurrentComponent(this);

        internalGetTemplateMap().clear();
        instanceIndexForCacheForGetInstance.clear();

        template = null;
        skipTemplate = false;
        outputStream = null;
        writer = null;
        validators = new LinkedHashMap<String, List<Validator>>();
        frameMap = new HashMap<String, String>();
        overrideParameters.clear();

        parametersInjector.inject(getRequest());

        put("frame", FrameDirective.getInstance());
        put("link", LinkDirective.getInstance());
        put("caption", CaptionDirective.getInstance());

        put("home", ApplicationContext.getInstance().getContextPath());
    }

    void finalizeAfterAction() {
        if (ApplicationContext.getInstance().isDebug()) {
            try {
                ReflectionUtil.invoke(ApplicationContext.getInstance(), "setCurrentComponent", parentComponent);
            } catch (ReflectionException e) {
                throw new NocturneException("Can't set current component back.", e);
            }
        } else {
            ApplicationContext.getInstance().setCurrentComponent(parentComponent);
        }
    }

    void initializeIfNeeded() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    init();
                    initialized = true;
                }
            }
        }
    }

    /**
     * Aborts execution and sends redirect to browser.
     *
     * @param target for redirection. It will be prepended with
     *               ApplicationContext.getInstance().getContextPath() if doen't contain absolute path.
     */
    public void abortWithRedirect(String target) {
        try {
            // make it absolute
            if (!target.startsWith("/") && !target.contains("://")) {
                target = ApplicationContext.getInstance().getContextPath() + "/" + target;
            }

            getResponse().sendRedirect(target);
        } catch (IOException e) {
            throw new ServletException("Can't redirect to " + target + ".", e);
        }
        throw new AbortException("Redirected to " + target + ".");
    }

    /**
     * Aborts execution and sends error to browser.
     *
     * @param code (for example 404, prefered to use HttpURLConnection.HTTP_* constants).
     */
    public void abortWithError(int code) {
        try {
            getResponse().sendError(code);
        } catch (IOException e) {
            throw new ServletException("Can't send error " + code + ".", e);
        }
        throw new AbortException("Send error [code = " + code + "].");
    }

    /**
     * @param pageClass Aborts current flow and redirects to the page pageClass.
     */
    public void abortWithRedirect(Class<? extends Page> pageClass) {
        abortWithRedirect(Links.getLink(pageClass));
    }

    /**
     * @param pageClass Aborts current flow and redirects to the page pageClass.
     * @param params    Map as an array (see Link.getLink()).
     */
    public void abortWithRedirect(Class<? extends Page> pageClass, Object... params) {
        abortWithRedirect(Links.getLink(pageClass, params));
    }

    /**
     * Redirects to the same page.
     */
    public void abortWithReload() {
        String url = getRequest().getRequestURL().toString();
        String queryString = getRequest().getQueryString();
        abortWithRedirect(url + (queryString != null ? "?" + queryString : ""));
    }

    /**
     * Adds validator to the specific parameter. It will be executed on
     * runValidation(). Validators storage will be cleared between controller
     * usages.
     *
     * @param parameter Parameter name.
     * @param validator Validator instance.
     * @return Component itself to support chains like
     *         addValidator("login", v1).addValidator("login", v2).
     */
    public Component addValidator(String parameter, Validator validator) {
        if (!validators.containsKey(parameter)) {
            validators.put(parameter, new ArrayList<Validator>());
        }

        validators.get(parameter).add(validator);
        return this;
    }

    private boolean runValidation(ErrorValidationHandler handler) {
        boolean failed = false;

        Map parameterMap = getRequestParams();
        for (Object key : parameterMap.keySet()) {
            Object value = parameterMap.get(key);
            if (value != null) {
                put(key.toString(), getString(key.toString()));
            } else {
                put(key.toString(), null);
            }
        }

        for (Map.Entry<String, List<Validator>> entry : validators.entrySet()) {
            String parameter = entry.getKey();
            List<Validator> parameterValidators = entry.getValue();

            if (parameterValidators != null) {
                String errorMessage = null;
                String errorParameter = "error__" + parameter;

                for (Validator validator : parameterValidators) {
                    try {
                        validator.run(getString(parameter));
                    } catch (ValidationException e) {
                        errorMessage = e.getMessage();
                        break;
                    }
                }

                if (errorMessage == null) {
                    remove(errorParameter);
                } else {
                    put(errorParameter, errorMessage);
                    handler.onError(parameter, errorMessage);
                    failed = true;
                }
            }

            put(parameter, getString(parameter));
        }

        return !failed;
    }

    /**
     * Runs all added validators.
     *
     * @return {@code true} iff all validators returns true. It will iterate through
     *         parameters in some order and invokes its validators in order of addition.
     *         If validator for parameter finds error, other validators for this parameter
     *         skipped and error message added as template
     *         variable "error__" + parameterName. Also all parameters added to template
     *         variables in this case.
     */
    public boolean runValidation() {
        return runValidation(new ErrorValidationHandler() {
            public void onError(String fieldName, String errorText) {
                // No operations.
            }
        });
    }

    /**
     * See runValidation(), but additionally it prints all the errors in JSON
     * to output writer. For example, it can print: {@code
     * {"error__login": "Login is already in use"}
     * }
     *
     * @return boolean {@code true} if validation passed.
     */
    public boolean runValidationAndPrintErrors() {
        final Map<String, String> errors = new LinkedHashMap<String, String>();

        boolean result = runValidation(new ErrorValidationHandler() {
            public void onError(String fieldName, String errorText) {
                errors.put("error__" + fieldName, errorText);
            }
        });

        if (errors.size() > 0) {
            synchronized (gson) {
                Type mapType = new TypeToken<Map<String, String>>() {
                }.getType();
                getResponse().setContentType("application/json");
                Writer writer = getWriter();
                try {
                    writer.write(gson.toJson(errors, mapType));
                    writer.flush();
                } catch (IOException e) {
                    // No operations.
                }
            }
        }

        return result;
    }

    /**
     * Prints all the key-values added by put(String, Object) inside this Component
     * as json into response writer.
     * <p/>
     * For each value (Object) will be called toString() and printed json data
     * will contain keys and values as strings.
     *
     * @param keys If at least one key is specified then the method take care only about
     *             templateMap entries (putted key-value pairs), such that key in keys array.
     *             If keys are not specified, method returns all the entries.
     */
    public void printTemplateMapAsStringsUsingJson(String... keys) {
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();

        Set<String> keySet = new HashSet<String>(Arrays.asList(keys));

        Map<String, String> params = new HashMap<String, String>();
        for (Map.Entry<String, Object> entry : templateMap.entrySet()) {
            String key = entry.getKey();
            if (keySet.isEmpty() || keySet.contains(key)) {
                Object value = entry.getValue();
                if (value != null) {
                    params.put(key, value.toString());
                }
            }
        }

        getResponse().setContentType("application/json");
        Writer writer = getWriter();
        try {
            writer.write(gson.toJson(params, mapType));
            writer.flush();
        } catch (IOException e) {
            // No operations.
        }
    }

    private Page getCurrentPage() {
        return ApplicationContext.getInstance().getCurrentPage();
    }

    /**
     * @param shortcut Shortcut value.
     * @return The same as {@code ApplicationContext.getInstance().$()}.
     */
    public static String $(String shortcut) {
        return ApplicationContext.getInstance().$(shortcut);
    }

    /**
     * @param shortcut Shortcut value.
     * @param args     Shortcut arguments.
     * @return The same as {@code ApplicationContext.getInstance().$()}.
     */
    public static String $(String shortcut, Object... args) {
        return ApplicationContext.getInstance().$(shortcut, args);
    }

    /**
     * Returns instance of class. Creates it via IoC injector
     * uses ApplicationContext.getInstance().getInjector().getInstance(clazz).
     * <p/>
     * But it doesn't create new instance for component if it already
     * was created. I.e. there is special map Map<Class<?>, List<Object>> to cache
     * instances. If inside single request there will be several calls
     * concretComponent.getInstance(ConcretClass.class), each of them returns
     * own instance. But the first call of concretComponent.getInstance(ConcretClass.class)
     * in the next request will return the same as the first call in the current request
     * and so on.
     *
     * @param clazz Instance class.
     * @return Returns new or cached instance.
     */
    @SuppressWarnings({"unchecked"})
    public synchronized <T> T getInstance(Class<T> clazz) {
        if (!cacheForGetInstance.containsKey(clazz)) {
            cacheForGetInstance.put(clazz, new ArrayList<Object>(2));
        }

        if (!instanceIndexForCacheForGetInstance.containsKey(clazz)) {
            instanceIndexForCacheForGetInstance.put(clazz, 0);
        }

        int index = instanceIndexForCacheForGetInstance.get(clazz);

        List<Object> clazzList = cacheForGetInstance.get(clazz);
        if (clazzList.size() <= index) {
            clazzList.add(ApplicationContext.getInstance().getInjector().getInstance(clazz));
        }
        instanceIndexForCacheForGetInstance.put(clazz, index + 1);

        return (T) clazzList.get(index);
    }

    /* init */ {
        synchronized (getClass()) {
            if (!actionMaps.containsKey(getClass())) {
                actionMaps.put(getClass(), new ActionMap(getClass()));
            }
        }
    }

    private interface ErrorValidationHandler {
        void onError(String fieldName, String errorText);
    }
}
