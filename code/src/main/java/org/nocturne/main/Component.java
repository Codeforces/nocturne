/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import net.sf.cglib.reflect.FastMethod;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.nocturne.cache.CacheHandler;
import org.nocturne.caption.CaptionDirective;
import org.nocturne.collection.SingleEntryList;
import org.nocturne.exception.*;
import org.nocturne.link.LinkDirective;
import org.nocturne.link.Links;
import org.nocturne.reset.FieldsResetter;
import org.nocturne.util.ReflectionUtil;
import org.nocturne.util.RequestUtil;
import org.nocturne.validation.ValidationException;
import org.nocturne.validation.Validator;

import javax.annotation.Nullable;
import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Base class for Page and Frame (controllers). You should not use
 * Component directly (creates subclasses of it).
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings({"DollarSignInName", "NonStaticInitializer", "NoopMethodInAbstractClass", "ClassReferencesSubclass", "OverloadedVarargsMethod"})
public abstract class Component {
    /**
     * Lock to synchronise some operations related to this component.
     */
    private final Lock componentLock = new ReentrantLock();

    /**
     * Lazy-initialized JSON converter for this component.
     */
    private Gson jsonConverter;

    /**
     * Has been initialized?
     */
    private volatile boolean initialized;

    /**
     * Default is null, which means no caching.
     */
    private CacheHandler cacheHandler;

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
    private Template template;

    /**
     * Map to store template variables.
     */
    private Map<String, Object> templateMap;

    /**
     * Map to store frame contents after parse().
     */
    private Map<String, String> frameMap = new HashMap<>();

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
    @SuppressWarnings("ThisEscapedInObjectConstruction")
    private final ParametersInjector parametersInjector = new ParametersInjector(this);

    /**
     * Parent component (needed for frames).
     */
    private Component parentComponent;

    /**
     * Http servlet response writer.
     */
    private PrintWriter writer;

    /**
     * Stores cached instances for #getInstance(clazz, index) method.
     */
    private final Map<Class<?>, List<Object>> cacheForGetInstance = new HashMap<>();

    /**
     * Stores current indices of instances for #getInstance(clazz).
     */
    private Map<Class<?>, Integer> instanceIndexForCacheForGetInstance;

    /**
     * Stores information about action, validation and invalid methods for component.
     */
    private static final ConcurrentMap<Class<? extends Component>, ActionMap> actionMaps = new ConcurrentHashMap<>();

    /**
     * Template file name: simple class name + ".ftl" by default.
     * If component class is instrumented via AOP (so inherited class auto-generated), the main class is used.
     */
    private String templateFileName = getTemplateFileName();

    /**
     * Map, containing parameters, which will be checked before request.getParameter().
     */
    private Map<String, List<String>> overrideParameters;

    /**
     * Stores params from request.
     */
    private Map<String, List<String>> requestParams = new HashMap<>();

    /**
     * Object to clean fields between requests.
     */
    private FieldsResetter fieldsResetter;

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
    protected static String getActionName() {
        return ApplicationContext.getInstance().getRequestAction();
    }

    /**
     * Use it to interrupt current method and skip validate/invalid/action methods (if any and not invoked yet).
     * Could be called from initializeAction, validate/invalid/action methods or finalizeAction.
     */
    protected void interrupt() {
        throw new InterruptException("Interrupted [componentClass=" + getClass().getName() + "].");
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
                validationResult = (Boolean) validateMethod.invoke(this, parametersInjector.setupParameters(request, validateMethod));
            }

            if (validationResult) {
                ActionMap.ActionMethod actionMethod = actionMap.getActionMethod(actionParameter);
                // TODO: Can't be applied now because of Codeforces frames.
                // ensureHttpMethod(actionMethod);
                if (actionMethod != null) {
                    actionMethod.getMethod().invoke(this, parametersInjector.setupParameters(request, actionMethod.getMethod()));
                } else {
                    throw new NocturneException("Can't find action method for component "
                            + getClass().getName() + " and action parameter = " + actionParameter + '.');
                }
            } else {
                FastMethod invalidMethod = actionMap.getInvalidMethod(actionParameter);
                if (invalidMethod != null) {
                    invalidMethod.invoke(this, parametersInjector.setupParameters(request, invalidMethod));
                }
            }
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof InterruptException) {
                throw (InterruptException) e.getCause();
            }

            if (e.getCause() instanceof AbortException) {
                throw (AbortException) e.getCause();
            }

            throw new NocturneException("Can't invoke validate or action method for component class "
                    + getClass().getName() + " [action=" + actionParameter + "].", e);
        }
    }

    private void ensureHttpMethod(ActionMap.ActionMethod actionMethod) {
        HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod().toUpperCase());

        if (actionMethod.getAction() == null && requestMethod != HttpMethod.GET) {
            abortWithError(HttpServletResponse.SC_BAD_REQUEST, "HTTP requestMethod GET is not supported by "
                    + getClass().getSimpleName() + '#' + actionMethod.getMethod().getName());
        }

        if (actionMethod.getAction() != null) {
            for (HttpMethod httpMethod : actionMethod.getAction().method()) {
                if (httpMethod == requestMethod) {
                    return;
                }
            }

            abortWithError(HttpServletResponse.SC_BAD_REQUEST, "HTTP requestMethod " + requestMethod
                    + " is not supported by "
                    + getClass().getSimpleName() + '#' + actionMethod.getMethod().getName());
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
                outputStream = response.getOutputStream();
            } catch (IOException e) {
                throw new ServletException("Can't get response output stream.", e);
            }
        }

        return outputStream;
    }

    /**
     * @return Http servlet response writer. Uses UTF-8 encoding. Invokes
     *         new PrintWriter(new OutputStreamWriter(getOutputStream(), java.nio.charset.StandardCharsets.UTF_8), true)
     *         but exactly once (uses lazy calculations).
     */
    public PrintWriter getWriter() {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(getOutputStream(), StandardCharsets.UTF_8), true);
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
            logger = Logger.getLogger(ReflectionUtil.getOriginalClass(getClass()));
        }

        return logger;
    }

    /**
     * @param key Session variable name.
     * @return returns true iff session contains attribute with name "key".
     */
    public boolean hasSession(String key) {
        try {
            HttpSession session = request.getSession(false);
            return session != null && session.getAttribute(key) != null;
        } catch (IllegalStateException ignored) {
            throw new SessionInvalidatedException();
        }
    }

    private void internalPutSession(String key, Serializable value) {
        getCurrentPage().putRequestCache(key, value);

        if (ApplicationContext.getInstance().isDebug()) {
            String json = getJsonConverter().toJson(value);
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
    public void putSession(String key, Serializable value) {
        try {
            internalPutSession(key, value);
        } catch (IllegalStateException ignored) {
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
        } catch (IllegalStateException ignored) {
            throw new SessionInvalidatedException();
        }
    }

    /**
     * @param <T>   Value class.
     * @param key   Session attribute name.
     * @param clazz Value class.
     * @return Value as instance of class "clazz". Use JSON as internal format to
     *         store values in the debug mode.
     */
    @SuppressWarnings({"unchecked"})
    public <T extends Serializable> T getSession(String key, Class<T> clazz) {
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
                    result = getJsonConverter().fromJson(json, clazz);
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
                } catch (Exception ignored) {
                    result = null;
                }
            }

            getCurrentPage().putRequestCache(key, result);

            return result;
        } catch (IllegalStateException ignored) {
            throw new SessionInvalidatedException();
        }
    }

    /**
     * @param <T>  Value class.
     * @param key  Session attribute name.
     * @param type Value type.
     * @return Value as instance of type "type". Use JSON as internal format to
     *         store values in the debug mode.
     */
    @SuppressWarnings({"unchecked", "RedundantTypeArguments"})
    public <T extends Serializable> T getSession(String key, Type type) {
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
                    result = getJsonConverter().<T>fromJson(json, type);
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
        } catch (IllegalStateException ignored) {
            throw new SessionInvalidatedException();
        }
    }

    void addOverrideParameter(String name, String value) {
        overrideParameters.put(name, new SingleEntryList<>(value));
    }

    void addOverrideParameter(String name, Collection<String> values) {
        overrideParameters.put(name, new ArrayList<>(values));
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
        if (skipTemplate) {
            template = null;
        } else {
            if (template == null || !template.getName().equals(templateFileName)) {
                try {
                    template = templateEngineConfiguration.getTemplate(templateFileName, ApplicationContext.getInstance().getLocale());
                } catch (IOException e) {
                    throw new FreemarkerException("Can't get freemarker template [name=" + templateFileName + "].", e);
                }
            }
        }
        //ApplicationContext.getInstance().setComponentByTemplate(template, this);
        return template;
    }

    private static void ensureArgumentIs(String key, Object value, String expectedKey, Object expectedValue) {
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
    public void put(String key, @Nullable Object value) {
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

    private static void validateParameter(String key, Object value) {
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
     * @return Is template processing will be skipped? It may be useful for AJAX pages.
     */
    boolean isSkipTemplate() {
        return skipTemplate;
    }

    /**
     * Call it if you don't want current component will
     * parse templates. It may be useful for AJAX pages.
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

    @SuppressWarnings("ReturnOfCollectionOrArrayField")
    Map<String, Object> internalGetTemplateMap() {
        return templateMap;
    }

    public Map<String, Object> getTemplateMap() {
        return new HashMap<>(templateMap);
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
     * @return Returns parameter as string.
     */
    public String getString(String key) {
        if (overrideParameters.containsKey(key)) {
            return RequestUtil.getFirst(overrideParameters, key);
        } else {
            return RequestUtil.getFirst(requestParams, key);
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as boolean. Returns {@code false}
     *         on invalid boolean value. Returns {@code true} if
     *         key is "true", "on", "yes", "y", "1" or "checked" (case insensitive).
     */
    public boolean getBoolean(String key) {
        String value = getString(key);
        return "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value) || "1".equalsIgnoreCase(value)
                || "y".equalsIgnoreCase(value) || "checked".equalsIgnoreCase(value);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as character. Returns {@code 0}
     *         on invalid character value.
     */
    public char getChar(String key) {
        String value = getString(key);
        return value == null || value.isEmpty() ? 0 : value.charAt(0);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as byte. Returns {@code 0}
     *         on invalid byte value.
     */
    public byte getByte(String key) {
        try {
            return Byte.parseByte(getString(key));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as short integer. Returns {@code 0}
     *         on invalid short integer value.
     */
    public short getShort(String key) {
        try {
            return Short.parseShort(getString(key));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as integer. Returns {@code 0}
     *         on invalid integer value.
     */
    public int getInteger(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException ignored) {
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
        } catch (NumberFormatException ignored) {
            return 0L;
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
        } catch (NumberFormatException ignored) {
            return 0.0F;
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
        } catch (NumberFormatException ignored) {
            return 0.0;
        }
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as string array.
     */
    public String[] getStrings(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (String[]) ParametersInjector.getArrayAssignValue(null, params.get(key), String[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as boolean array.
     */
    public boolean[] getBooleans(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (boolean[]) ParametersInjector.getArrayAssignValue(null, params.get(key), boolean[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as character array.
     */
    public char[] getChars(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (char[]) ParametersInjector.getArrayAssignValue(null, params.get(key), char[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as byte array.
     */
    public byte[] getBytes(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (byte[]) ParametersInjector.getArrayAssignValue(null, params.get(key), byte[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as short integer array.
     */
    public short[] getShorts(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (short[]) ParametersInjector.getArrayAssignValue(null, params.get(key), short[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as integer array.
     */
    public int[] getIntegers(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (int[]) ParametersInjector.getArrayAssignValue(null, params.get(key), int[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as long integer array.
     */
    public long[] getLongs(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (long[]) ParametersInjector.getArrayAssignValue(null, params.get(key), long[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as float number array.
     */
    public float[] getFloats(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (float[]) ParametersInjector.getArrayAssignValue(null, params.get(key), float[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns parameter as double number array.
     */
    public double[] getDoubles(String key) {
        Map<String, List<String>> params = overrideParameters.containsKey(key) ? overrideParameters : requestParams;
        return (double[]) ParametersInjector.getArrayAssignValue(null, params.get(key), double[].class);
    }

    /**
     * @param key Parameter name.
     * @return Returns {@code true} if there is such parameter and
     *         it differs from {@code null}.
     */
    protected boolean hasParameter(String key) {
        return getString(key) != null;
    }

    Map<String, List<String>> getRequestParams() {
        return new HashMap<>(requestParams);
    }

    void setRequest(HttpServletRequest request) {
        this.request = request;
        setupRequestParams();
    }

    protected void setupRequestParams() {
        throw new NocturneException("Expected Page or Frame class.");
    }

    protected final void setupRequestParamsForPage() {
        requestParams = RequestUtil.getRequestParams(request);
    }

    protected final void setupRequestParamsForFrame() {
        requestParams = ApplicationContext.getInstance().getCurrentPage().getRequestParams();
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
        frame.setRequest(request);
        frame.setResponse(response);
        frame.setFilterConfig(filterConfig);
        frame.setTemplateEngineConfiguration(templateEngineConfiguration);
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

        templateMap = Collections.synchronizedMap(new HashMap<String, Object>());
        instanceIndexForCacheForGetInstance = Collections.synchronizedMap(new HashMap<Class<?>, Integer>());
        template = null;
        skipTemplate = false;
        outputStream = null;
        writer = null;
        validators = new LinkedHashMap<>();
        frameMap = new HashMap<>();
        overrideParameters = Collections.synchronizedMap(new HashMap<String, List<String>>());

        parametersInjector.inject(request);

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

        resetFields();
    }

    void resetFields() {
        if (fieldsResetter == null) {
            fieldsResetter = new FieldsResetter(this);
        }

        fieldsResetter.resetFields();
    }

    void initializeIfNeeded() {
        if (!initialized) {
            componentLock.lock();
            try {
                if (!initialized) {
                    init();
                    initialized = true;
                }
            } finally {
                componentLock.unlock();
            }
        }
    }

    /**
     * Aborts execution and sends redirect to browser.
     *
     * @param target for redirection. It will be prepended with
     *               ApplicationContext.getInstance().getContextPath() if doesn't contain absolute path.
     */
    @Contract("_ -> fail")
    public void abortWithRedirect(String target) {
        try {
            // make it absolute
            if (!target.startsWith("/") && !target.contains("://")) {
                target = ApplicationContext.getInstance().getContextPath() + '/' + target;
            }

            response.sendRedirect(target);
        } catch (IOException e) {
            throw new ServletException("Can't redirect to " + target + '.', e);
        }
        throw new AbortException("Redirected to " + target + '.', target);
    }

    /**
     * Aborts execution and sends error to browser.
     *
     * @param code    (for example 404, preferred to use HttpURLConnection.HTTP_* constants).
     * @param message Error message.
     */
    @Contract("_, _ -> fail")
    public void abortWithError(int code, String message) {
        try {
            response.sendError(code, message);
        } catch (IOException e) {
            throw new ServletException("Can't send error " + code + '.', e);
        }
        throw new AbortException("Send error [code = " + code + ", message = \"" + message + "\"].");
    }

    /**
     * Aborts execution and sends error to browser.
     *
     * @param code (for example 404, preferred to use HttpURLConnection.HTTP_* constants).
     */
    @Contract("_ -> fail")
    public void abortWithError(int code) {
        try {
            response.sendError(code);
        } catch (IOException e) {
            throw new ServletException("Can't send error " + code + '.', e);
        }
        throw new AbortException("Send error [code = " + code + "].");
    }

    /**
     * @param pageClass Aborts current flow and redirects to the page pageClass.
     */
    @Contract("_ -> fail")
    public void abortWithRedirect(Class<? extends Page> pageClass) {
        abortWithRedirect(Links.getLink(pageClass));
    }

    /**
     * @param pageClass Aborts current flow and redirects to the page pageClass.
     * @param params    Map as an array (see Link.getLink()).
     */
    @Contract("_, _ -> fail")
    public void abortWithRedirect(Class<? extends Page> pageClass, Object... params) {
        abortWithRedirect(Links.getLink(pageClass, params));
    }

    /**
     * Redirects to the same page.
     */
    @Contract("-> fail")
    public void abortWithReload() {
        String url = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        abortWithRedirect(url + (queryString != null ? '?' + queryString : ""));
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
        Map parameterMap = getRequestParams();
        for (Object entryObject : parameterMap.entrySet()) {
            Map.Entry entry = (Map.Entry) entryObject;
            Object key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                put(key.toString(), null);
            } else {
                setupTemplateMapByParameter(key.toString());
            }
        }

        boolean failed = false;

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

            setupTemplateMapByParameter(parameter);
        }

        return !failed;
    }

    private void setupTemplateMapByParameter(String parameter) {
        Object previousValue = getTemplateMap().get(parameter);
        String value = getString(parameter);
        if (previousValue == null || !previousValue.toString().equals(value)) {
            put(parameter, getString(parameter));
        }
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
            @Override
            public void onError(String fieldName, String errorText) {
                // No operations.
            }
        });
    }

    /**
     * See runValidation(), but additionally it prints all the errors in JSON
     * to output writer. For example, it can print:
     * <pre>
     * {"error__login": "Login is already in use"}
     * </pre>
     *
     * @return boolean {@code true} if validation passed.
     */
    public boolean runValidationAndPrintErrors() {
        final Map<String, String> errors = new LinkedHashMap<>();

        boolean result = runValidation(new ErrorValidationHandler() {
            @Override
            public void onError(String fieldName, String errorText) {
                errors.put("error__" + fieldName, errorText);
            }
        });

        if (!errors.isEmpty()) {
            Type mapType = new TypeToken<Map<String, String>>() {
            }.getType();
            response.setContentType("application/json");
            Writer localWriter = getWriter();
            try {
                localWriter.write(getJsonConverter().toJson(errors, mapType));
                localWriter.flush();
            } catch (IOException ignored) {
                // No operations.
            }
        }

        return result;
    }

    /**
     * <p>
     * Prints all the key-values added by put(String, Object) inside this Component
     * as json into response writer.
     * </p>
     * <p>
     * For each value (Object) will be called toString() and printed json data
     * will contain keys and values as strings.
     * </p>
     *
     * @param keys If at least one key is specified then the method takes care only about
     *             templateMap entries (putted key-value pairs), such that key is in keys array.
     *             If keys are not specified, method returns all the entries.
     */
    public void printTemplateMapAsStringsUsingJson(String... keys) {
        Type mapType = new TypeToken<Map<String, String>>() {
        }.getType();

        Set<String> keySet = new HashSet<>(Arrays.asList(keys));

        Map<String, String> params = new HashMap<>();
        for (Map.Entry<String, Object> entry : templateMap.entrySet()) {
            String key = entry.getKey();
            if (keySet.isEmpty() || keySet.contains(key)) {
                Object value = entry.getValue();
                if (value != null) {
                    params.put(key, value.toString());
                }
            }
        }

        response.setContentType("application/json");
        Writer localWriter = getWriter();
        try {
            localWriter.write(getJsonConverter().toJson(params, mapType));
            localWriter.flush();
        } catch (IOException ignored) {
            // No operations.
        }
    }

    private static Page getCurrentPage() {
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
     * <p>
     * Returns instance of class. Creates it via IoC injector
     * uses ApplicationContext.getInstance().getInjector().getInstance(clazz).
     * </p>
     * <p>
     * But it doesn't create new instance for component if it already
     * was created. I.e. there is special map {@literal Map<Class<?>, List<Object>>} to cache
     * instances. If inside single request there will be several calls
     * concreteComponent.getInstance(ConcreteClass.class), each of them returns
     * own instance. But the first call of concreteComponent.getInstance(ConcreteClass.class)
     * in the next request will return the same as the first call in the current request
     * and so on.
     * </p>
     *
     * @param <T>   Instance class.
     * @param clazz Instance class.
     * @return Returns new or cached instance.
     */
    @SuppressWarnings({"unchecked"})
    public <T> T getInstance(Class<T> clazz) {
        componentLock.lock();
        try {
            Integer index = instanceIndexForCacheForGetInstance.get(clazz);
            if (index == null) {
                index = 0;
                instanceIndexForCacheForGetInstance.put(clazz, index);
            }

            List<Object> clazzList = cacheForGetInstance.get(clazz);
            if (clazzList == null) {
                clazzList = new ArrayList<>(2);
                cacheForGetInstance.put(clazz, clazzList);
            }

            if (clazzList.size() <= index) {
                clazzList.add(ApplicationContext.getInstance().getInjector().getInstance(clazz));
            }
            instanceIndexForCacheForGetInstance.put(clazz, index + 1);

            return (T) clazzList.get(index);
        } finally {
            componentLock.unlock();
        }
    }

    private String getTemplateFileName() {
        Class<?> clazz = getClass();
        while (clazz.getSimpleName().contains("$")) {
            clazz = clazz.getSuperclass();
        }
        return clazz.getSimpleName() + ".ftl";
    }

    protected final Gson getJsonConverter() {
        if (jsonConverter == null) {
            jsonConverter = new Gson();
        }
        return jsonConverter;
    }

    /* init */ {
        synchronized (getClass()) {
            if (!actionMaps.containsKey(getClass())) {
                actionMaps.putIfAbsent(getClass(), new ActionMap(getClass()));
            }
        }
    }

    /**
     * Wraps object using {@code {@link BeansWrapper BeansWrapper}}.
     *
     * @param object to wrap
     * @return wrapped object
     * @throws TemplateModelException if {@code {@link BeansWrapper#wrap(Object) BeansWrapper.wrap(object)}}
     *                                throws an exception
     */
    protected final TemplateModel wrapBean(Object object) throws TemplateModelException {
        return new BeansWrapperBuilder(Constants.FREEMARKER_VERSION).build().wrap(object);
    }

    /**
     * Wraps object using {@code {@link BeansWrapper BeansWrapper}} and suppresses checked exception.
     *
     * @param object to wrap
     * @return wrapped object
     * @throws NocturneException if {@code {@link BeansWrapper#wrap(Object) BeansWrapper.wrap(object)}}
     *                           throws an {@code {@link TemplateModelException TemplateModelException}}
     */
    protected final TemplateModel wrapBeanUnchecked(Object object) {
        try {
            return new BeansWrapperBuilder(Constants.FREEMARKER_VERSION).build().wrap(object);
        } catch (TemplateModelException e) {
            error("Can't wrap object '" + object + "' using BeansWrapper.", e);
            throw new NocturneException("Can't wrap object '" + object + "' using BeansWrapper.", e);
        }
    }

    private interface ErrorValidationHandler {
        void onError(String fieldName, String errorText);
    }
}
