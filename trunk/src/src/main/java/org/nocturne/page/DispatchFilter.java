package org.nocturne.page;

import freemarker.template.Configuration;
import org.nocturne.misc.ApplicationContext;
import org.nocturne.misc.ReloadingClassLoader;
import org.nocturne.pool.TemplateEngineConfigurationPool;
import org.apache.log4j.Logger;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Properties;

/** @author Mike Mirzayanov */
public class DispatchFilter implements Filter {
    /** Logger. */
    private static Logger logger = Logger.getLogger(DispatchFilter.class);

    /**
     * {@link org.nocturne.misc.ApplicationContext}
     * Stores different paths and other properties.
     */
    private final ApplicationContext applicationContext
            = new ApplicationContext();

    /** Freemarker configuration. */
    private TemplateEngineConfigurationPool templateEngineConfigurationPool;

    /** Page loader for production mode. */
    private PageLoader pageLoader
            = new PageLoader(applicationContext);

    /** Servlet config. */
    private FilterConfig filterConfig;

    /** Listens requests for pages. */
    private PageRequestListener pageRequestListener;

    /** When application has been accessed in the debug mode. */
    private long lastDebugModeAccess = 0;

    /**
     * Hash code of all directories in the reloading class path
     * used when application has been accessed in the debug mode.
     */
    private long lastDebugModeAccessReloadingClassPathHashCode = 0;

    /** Class loader used when application has been accessed in the debug mode. */
    private ClassLoader lastDebugModeClassLoader;

    /** @return ApplicationContext Nocturne's application context. */
    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * Run production mode request handling.
     *
     * @param request  Request.
     * @param response Response.
     * @return Page run result.
     * @throws IOException when Something wrong with IO.
     */
    @SuppressWarnings({"unchecked"})
    private RunResult runProductionService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getServletPath();
        Map<String, String> parameterMap = request.getParameterMap();

        Page page = pageLoader.loadPage(path, parameterMap);
        Configuration templateEngineConfiguration = templateEngineConfigurationPool.getInstance();

        boolean processChain;
        Throwable pageThrowable = null;

        try {
            page.setApplicationContext(applicationContext);
            page.setTemplateEngineConfiguration(templateEngineConfiguration);
            page.setRequest(request);
            page.setFilterConfig(getFilterConfig());
            page.setResponse(response);

            setupPageRequestListener(page);
            handleBeforeProcessPage(page);

            page.parseTemplate();
            processChain = page.isProcessChain();

            try {
                page.getOutputStream().flush();
            } catch (Exception e) {
                try {
                    page.getOutputStream().close();
                } catch (Exception t) {
                    // No operations.
                }
            }
        } catch (Throwable e) {
            pageThrowable = e;
            e.printStackTrace();
            logger.fatal("Can't process " + request.getRequestURL() + ".", e);
            throw new IllegalStateException(e);
        } finally {
            handleAfterProcessPage(page, pageThrowable);
            pageLoader.unloadPage(path, parameterMap, page);
            templateEngineConfigurationPool.release(templateEngineConfiguration);
        }

        RunResult result = new RunResult();
        result.setProcessChain(processChain);

        return result;
    }

    private void setupPageRequestListener(Page page) throws ClassNotFoundException {
        if (pageRequestListener == null || applicationContext.isDebugMode()) {
            if (applicationContext.getPageRequestListenerClassName() != null) {
                ClassLoader loader = page.getClass().getClassLoader();
                Class<?> clazz = loader.loadClass(applicationContext.getPageRequestListenerClassName());
                pageRequestListener = (PageRequestListener) applicationContext.getInjector().getInstance(clazz);
            }
        }
    }

    private void handleBeforeProcessPage(Page page) {
        if (pageRequestListener != null) {
            pageRequestListener.beforeProcessPage(page);
        }
    }

    private void handleAfterProcessPage(Page page, Throwable t) {
        if (pageRequestListener != null) {
            pageRequestListener.afterProcessPage(page, t);
        }
    }

    /** @return filterConfig Retuns filter configuration instance. */
    private FilterConfig getFilterConfig() {
        return filterConfig;
    }

    /**
     * Run debug mode request handling.
     *
     * @param request  Request.
     * @param response Response.
     * @return RunResult page run result.
     */
    private RunResult runDebugService(HttpServletRequest request, HttpServletResponse response) {
        ClassLoader loader;

        if (System.currentTimeMillis() - lastDebugModeAccess < 1000 && lastDebugModeClassLoader != null) {
            loader = lastDebugModeClassLoader;
        } else {
            long hashCode = hashCode(applicationContext.getReloadingClassLoaderClassesPath());

            if (hashCode == lastDebugModeAccessReloadingClassPathHashCode) {
                loader = lastDebugModeClassLoader;
            } else {
                loader = new ReloadingClassLoader(applicationContext);
                lastDebugModeAccess = System.currentTimeMillis();
                lastDebugModeAccessReloadingClassPathHashCode = hashCode;
                lastDebugModeClassLoader = loader;
            }
        }

        RunResult runResult = new RunResult();

        Page page = null;
        Throwable pageThrowable = null;

        try {
            Class pageLoaderClass = loader.loadClass("org.nocturne.page.PageLoader");
            Object pageLoader = pageLoaderClass.newInstance();
            invoke(pageLoader, "setApplicationContext", applicationContext);
            page = (Page) invoke(pageLoader, "loadPage", request.getServletPath(), request.getParameterMap());
            processPage(request, response, page, runResult);
        } catch (Throwable e) {
            pageThrowable = e;
            e.printStackTrace();
            logger.fatal("Can't process " + request.getRequestURL() + ".", e);
            throw new IllegalStateException(e);
        } finally {
            if (page != null) {
                handleAfterProcessPage(page, pageThrowable);
            }
        }

        applicationContext.setInjector(null);

        return runResult;
    }

    /**
     * Setups page fields and calls specific methods.
     *
     * @param request   Request.
     * @param response  Response.
     * @param page      Page instance.
     * @param runResult Run result to be modified during processing.
     * @throws IOException            when fails IO.
     * @throws ClassNotFoundException If requested classes not found.
     */
    private void processPage(HttpServletRequest request, HttpServletResponse response, Page page, RunResult runResult) throws IOException, ClassNotFoundException {
        Configuration templateEngineConfiguration = templateEngineConfigurationPool.getInstance();

        try {
            invoke(page, "setApplicationContext", applicationContext);
            invoke(page, "setTemplateEngineConfiguration", templateEngineConfiguration);
            invoke(page, "setRequest", request);
            invoke(page, "setResponse", response);
            invoke(page, "setFilterConfig", getFilterConfig());

            setupPageRequestListener(page);
            handleBeforeProcessPage(page);
            invoke(page, "parseTemplate");
            runResult.setProcessChain((Boolean) invoke(page, "isProcessChain"));
        } finally {
            templateEngineConfigurationPool.release(templateEngineConfiguration);
        }
    }

    /**
     * Invokes method by name for object, finds method among methods
     * of specified class.
     *
     * @param clazz      Class instance where to find method.
     * @param object     Object which method will be invoked.
     * @param methodName Method name.
     * @param args       Method arguments.
     * @return Object Method return value.
     */
    private Object invoke(Class<?> clazz, Object object, String methodName, Object... args) {
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            if (method.getName().equals(methodName) &&
                    method.getParameterTypes().length == args.length) {
                try {
                    return method.invoke(object, args);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        if (clazz.getSuperclass() == null) {
            throw new IllegalStateException("Can't find method.");
        } else {
            return invoke(clazz.getSuperclass(), object, methodName, args);
        }
    }

    /**
     * Invokes method by name.
     *
     * @param object     Object which method will be invoked.
     * @param methodName Method name.
     * @param args       Method arguments.
     * @return Object Method return value.
     */
    private Object invoke(Object object, String methodName, Object... args) {
        Class<?> clazz = object.getClass();
        return invoke(clazz, object, methodName, args);
    }

    /**
     * Initializes nocturne application.
     * Reads configuration parameters from web.xml.
     *
     * @param config Config.
     * @throws ServletException when method fails.
     */
    public void init(FilterConfig config) throws ServletException {
        templateEngineConfigurationPool
                = new TemplateEngineConfigurationPool(applicationContext, config);

        Properties properties = new Properties();
        filterConfig = config;

        try {
            properties.load(DispatchFilter.class.getResourceAsStream("/web.properties"));
        } catch (IOException e) {
            throw new ServletException("Can't load file /web.properties.", e);
        }

        // application context initialization
        applicationContext.setTemplatesPath(properties.getProperty("nocturne.templates-path"));
        applicationContext.setDebugMode(properties.getProperty("nocturne.debug").equals("true"));
        applicationContext.setGuiceModuleClassName(config.getInitParameter("nocturne.guice-module-class-name"));
        applicationContext.setReloadingClassLoaderClassesPath(properties.getProperty("nocturne.reloading-class-loader-classes-path"));
        applicationContext.setPageClassNameResolver(config.getInitParameter("nocturne.page-class-name-resolver"));
        applicationContext.setSkipRegex(config.getInitParameter("nocturne.skip-regex"));
        applicationContext.setReloadingClassLoaderPattern(config.getInitParameter("nocturne.reloading-class-loader-pattern"));
        applicationContext.setServletContext(config.getServletContext());

        if (properties.containsKey("nocturne.page-request-listener")) {
            applicationContext.setPageRequestListenerClassName(properties.getProperty("nocturne.page-request-listener"));
        }

        // Pass application context to servlet
        config.getServletContext().setAttribute("applicationContext", applicationContext);

        // Log.
        logger.info("Filter initialized.");
    }

    /**
     * Handles requests to the application pages.
     *
     * @param servletRequest  Request.
     * @param servletResponse Response.
     * @param filterChain     Filter chain.
     * @throws ServletException when method fails.
     * @throws IOException      when something wrong with IO.
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            String path = request.getServletPath();

            if (path.matches(applicationContext.getSkipRegex())) {
                filterChain.doFilter(servletRequest, servletResponse);
            } else {
                response.setContentType("text/html");
                ComponentLocator.clear();

                RunResult runResult;

                if (applicationContext.isDebugMode()) {
                    runResult = runDebugService(request, response);
                } else {
                    runResult = runProductionService(request, response);
                }

                ComponentLocator.clear();

                if (runResult.isProcessChain()) {
                    filterChain.doFilter(servletRequest, servletResponse);
                }
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    /** Destroy filter. */
    public void destroy() {
        templateEngineConfigurationPool.close();
        pageLoader.close();
    }

    private static class RunResult {
        private boolean processChain;

        public boolean isProcessChain() {
            return processChain;
        }

        public void setProcessChain(boolean processChain) {
            this.processChain = processChain;
        }
    }

    private long hashCode(String paths) {
        long result = 0;
        String[] tokens = paths.split(";");
        for (String token : tokens) {
            if (token.length() > 0) {
                File dir = new File(token);
                result += token.hashCode() * hashCode(dir, 0);
            }
        }
        return result;
    }

    private long hashCode(File file, long depth) {
        long result = 0;
        if (file.isFile()) {
            result += file.getName().hashCode() * file.lastModified() * (depth + 1);
        } else {
            File[] files = file.listFiles();
            for (File nested : files) {
                result += hashCode(nested, depth + 1);
            }
        }
        return result;
    }

    static {
        logger.info("Filter initializing.");
    }
}
