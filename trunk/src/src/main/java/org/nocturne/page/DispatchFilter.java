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

    /**
     * Run production mode request handling.
     *
     * @param request  Request.
     * @param response Response.
     * @throws IOException when Something wrong with IO.
     */
    private void runProductionService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getServletPath();
        Map<String, String> parameterMap = request.getParameterMap();

        Page page = pageLoader.loadPage(path, parameterMap);
        Configuration templateEngineConfiguration = templateEngineConfigurationPool.getInstance();
        ComponentLocator.setPage(page);

        try {
            page.setApplicationContext(applicationContext);
            page.setTemplateEngineConfiguration(templateEngineConfiguration);
            page.setRequest(request);
            page.setFilterConfig(getFilterConfig());
            page.setResponse(response);
            page.parseTemplate();

            page.getOutputStream().flush();
        } catch (Throwable e) {
            e.printStackTrace();
            logger.fatal("Can't process " + request.getRequestURL() + ".", e);
            throw new IllegalStateException(e);
        } finally {
            pageLoader.unloadPage(path, parameterMap, page);
            templateEngineConfigurationPool.release(templateEngineConfiguration);
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
     */
    private void runDebugService(HttpServletRequest request, HttpServletResponse response) {
        ReloadingClassLoader loader = new ReloadingClassLoader(applicationContext);

        try {
            Class pageLoaderClass = loader.loadClass("org.nocturne.page.PageLoader");
            Object pageLoader = pageLoaderClass.newInstance();
            invoke(pageLoader, "setApplicationContext", applicationContext);
            Object page = invoke(pageLoader, "loadPage", request.getServletPath(), request.getParameterMap());
            processPage(request, response, page);
        } catch (Throwable e) {
            e.printStackTrace();
            logger.fatal("Can't process " + request.getRequestURL() + ".", e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * Setups page fields and calls specific methods.
     *
     * @param request  Request.
     * @param response Response.
     * @param page     Page instance.
     * @throws IOException when fails IO.
     */
    private void processPage(HttpServletRequest request, HttpServletResponse response, Object page) throws IOException {
        ComponentLocator.setPage((Page) page);
        Configuration templateEngineConfiguration = templateEngineConfigurationPool.getInstance();

        try {
            invoke(page, "setApplicationContext", applicationContext);
            invoke(page, "setTemplateEngineConfiguration", templateEngineConfiguration);
            invoke(page, "setRequest", request);
            invoke(page, "setResponse", response);
            invoke(page, "setFilterConfig", getFilterConfig());
            invoke(page, "parseTemplate");
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

                if (applicationContext.isDebugMode()) {
                    runDebugService(request, response);
                } else {
                    runProductionService(request, response);
                }

                ComponentLocator.clear();
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

    static {
        logger.info("Filter initializing.");
    }
}
