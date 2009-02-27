package org.nocturne.page;

import com.google.gson.Gson;
import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.log4j.Logger;
import org.nocturne.misc.AbortException;
import org.nocturne.misc.ApplicationContext;
import org.nocturne.page.validation.ValidationException;
import org.nocturne.page.validation.Validator;

import javax.servlet.FilterConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.util.*;

/** @author Mike Mirzayanov */
public abstract class Component {
    private boolean initialized;

    private Gson gson = new Gson();

    private HttpSession session;

    private Logger logger;

    private Configuration templateEngineConfiguration;

    private Template template = null;

    private Map<String, Object> templateMap = new HashMap<String, Object>();

    private Map<String, String> frameMap = new HashMap<String, String>();

    private ApplicationContext applicationContext;

    private boolean skipTemplate;

    private FilterConfig filterConfig;

    private HttpServletRequest request;

    private HttpServletResponse response;

    private Map<String, List<Validator>> validators;

    public void addCss(String css) {
        ComponentLocator.getPage().getCssSet().add(css);
    }

    public void addJs(String js) {
        ComponentLocator.getPage().getJsSet().add(js);
    }

    public PrintWriter getWriter() throws IOException {
        return response.getWriter();
    }

    public Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(this.getClass());
        }

        return logger;
    }

    public boolean hasSession(String key) {
        return session.getAttribute(key) != null;
    }

    public void putSession(String key, Object value) {
        ensureSession();

        ComponentLocator.getPage().putRequestCache(key, value);

        if (applicationContext.isDebugMode()) {
            String json = gson.toJson(value);
            session.setAttribute(key, json);
        } else {
            session.setAttribute(key, value);
        }
    }

    public void removeSession(String key) {
        ComponentLocator.getPage().removeRequestCache(key);
        session.removeAttribute(key);
    }

    public <T> T getSession(String key, Class<T> clazz) {
        ensureSession();

        Object fromRequestCache = ComponentLocator.getPage().getRequestCache(key);

        if (fromRequestCache != null) {
            return (T) fromRequestCache;
        }

        T result;

        if (applicationContext.isDebugMode()) {
            String json = (String) session.getAttribute(key);
            if (json != null) {
                result = gson.fromJson(json, clazz);
            } else {
                result = null;
            }
        } else {
            try {
                result = (T) session.getAttribute(key);
            } catch (Exception e) {
                result = null;
            }
        }

        ComponentLocator.getPage().putRequestCache(key, result);

        return result;
    }

    public <T> T getSession(String key, Type type) {
        ensureSession();

        Object fromRequestCache = ComponentLocator.getPage().getRequestCache(key);

        if (fromRequestCache != null) {
            return (T) fromRequestCache;
        }

        T result;

        if (applicationContext.isDebugMode()) {
            String json = (String) session.getAttribute(key);
            if (json != null) {
                result = gson.<T>fromJson(json, type);
            } else {
                result = null;
            }
        } else {
            result = (T) session.getAttribute(key);
        }

        ComponentLocator.getPage().putRequestCache(key, result);

        return result;
    }

    private void ensureSession() {
        if (session == null) {
            session = request.getSession();
        }
    }

    public FilterConfig getFilterConfig() {
        return filterConfig;
    }

    void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    Configuration getTemplateEngineConfiguration() {
        return templateEngineConfiguration;
    }

    void setTemplateEngineConfiguration(Configuration templateEngineConfiguration) {
        this.templateEngineConfiguration = templateEngineConfiguration;
    }

    protected Template getTemplate() throws IOException {
        if (template == null) {
            String name = this.getClass().getSimpleName() + ".ftl";
            template = getTemplateEngineConfiguration().getTemplate(name);
        }
        ComponentLocator.set(template, this);
        return template;
    }

    private void ensureArgumentIs(String key, Object value, String expectedKey, Object expectedValue) {
        if (key.equals(expectedKey) && !expectedValue.equals(value)) {
            throw new IllegalArgumentException("Argument '" + key + "' is reserved for internal usage.");
        }
    }

    public void remove(String key) {
        getTemplateMap().remove(key);
    }

    public void put(String key, Object value) {
        validateParameter(key, value);

        getTemplateMap().put(key, value);
    }

    public void putGlobal(String key, Object value) {
        validateParameter(key, value);

        ComponentLocator.getPage().getGlobalTemplateMap().put(key, value);
    }

    private void validateParameter(String key, Object value) {
        ensureArgumentIs(key, value, "frame", FrameDirective.getInstance());
        ensureArgumentIs(key, value, "css", ComponentLocator.getPage().getCssSet());
        ensureArgumentIs(key, value, "js", ComponentLocator.getPage().getJsSet());
    }

    public Object get(String key) {
        return getTemplateMap().get(key);
    }

    protected void setTemplateName(String name) throws IOException {
        template = getTemplateEngineConfiguration().getTemplate(name);
    }

    boolean isSkipTemplate() {
        return skipTemplate;
    }

    protected void skipTemplate() {
        skipTemplate = true;
    }

    Map<String, Object> getTemplateMap() {
        return templateMap;
    }

    protected void debug(String format, Object... args) {
        getLogger().debug(String.format(format, args));
    }

    protected void info(String format, Object... args) {
        getLogger().info(String.format(format, args));
    }

    protected void warn(String format, Object... args) {
        getLogger().warn(String.format(format, args));
    }

    protected void error(String format, Object... args) {
        getLogger().error(String.format(format, args));
    }

    protected void fatal(String format, Object... args) {
        getLogger().fatal(String.format(format, args));
    }

    public String getString(String key) {
        return request.getParameter(key);
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }

    public int getInteger(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public long getLong(String key) {
        try {
            return Long.parseLong(getString(key));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    public double getDouble(String key) {
        try {
            return Double.parseDouble(getString(key));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    protected boolean hasParameter(String key) {
        return getString(key) != null;
    }

    void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void init() {
        // No operations.        
    }

    public void beforeRender() throws IOException {
    }

    public abstract void render() throws IOException;

    public void afterRender() throws IOException {
    }

    void setup(Frame frame) {
        frame.setRequest(getRequest());
        frame.setResponse(getResponse());
        frame.setFilterConfig(getFilterConfig());
        frame.setApplicationContext(getApplicationContext());
        frame.setTemplateEngineConfiguration(getTemplateEngineConfiguration());
    }

    String getFrameHtml(String key) {
        return frameMap.get(key);
    }

    public void parse(String key, Frame frame) throws IOException {
        setup(frame);
        frameMap.put(key, frame.parseTemplate());
    }

    void prepareForRender() {
        initializeIfNeeded();

        getTemplateMap().clear();
        validators = new HashMap<String, List<Validator>>();

        put("frame", FrameDirective.getInstance());
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

    public void abortWithRedirect(String target) throws IOException {
        getResponse().sendRedirect(target);
        throw new AbortException("Redirected to " + target + ".");
    }

    public void abortWithError(int code) throws IOException {
        getResponse().sendError(code);
        throw new AbortException("Send error [code = " + code + "].");
    }

    public Component addValidator(String parameter, Validator validator) {
        if (!validators.containsKey(parameter)) {
            validators.put(parameter, new ArrayList<Validator>());
        }

        validators.get(parameter).add(validator);
        return this;
    }

    public boolean validate() {
        boolean failed = false;

        Iterator<Map.Entry<String, List<Validator>>> i = validators.entrySet().iterator();
        while (i.hasNext()) {
            Map.Entry<String, List<Validator>> entry = i.next();

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
                    failed = true;
                }
            }

            put(parameter, getString(parameter));
        }

        return !failed;
    }
}
