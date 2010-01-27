/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import freemarker.template.Configuration;
import org.apache.log4j.Logger;
import org.nocturne.exception.NocturneException;
import org.nocturne.exception.ReflectionException;
import org.nocturne.listener.PageRequestListener;
import org.nocturne.module.Module;
import org.nocturne.pool.TemplateEngineConfigurationPool;
import org.nocturne.util.ReflectionUtil;
import org.nocturne.util.RequestUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author Mike Mirzayanov
 */
public class RequestDispatcher {
    /**
     * Logger.
     */
    private static Logger logger = Logger.getLogger(DispatchFilter.class);

    /**
     * Context.
     */
    private final ApplicationContext applicationContext
            = ApplicationContext.getInstance();

    /**
     * Freemarker configuration.
     */
    private TemplateEngineConfigurationPool templateEngineConfigurationPool;

    /**
     * Page loader for production mode.
     */
    private PageLoader pageLoader
            = new PageLoader();

    /**
     * Servlet config.
     */
    private FilterConfig filterConfig;

    /**
     * Listens requests for pages.
     */
    private List<Object> pageRequestListeners;

    /**
     * Class loader used when application has been accessed in the debug mode.
     */
    private ClassLoader reloadingClassLoader;

    /**
     * Runs init() method for all modules.
     * Each module should be initialized on the application startup.
     */
    private void initializeModules() {
        List<Module> modules = getModules();

        //TODO: To check order of initialization
        for (Module module : modules) {
            module.init();
        }

        Collections.sort(modules, new Comparator<Module>() {
            /**
             * Sorts by priority.
             *
             * @param a Module.
             * @param b Module.
             * @return int.
             */
            public int compare(Module a, Module b) {
                if (b.getPriority() != a.getPriority()) {
                    return b.getPriority() - a.getPriority();
                } else {
                    return a.getName().compareTo(b.getName());
                }
            }
        });

        for (Module module : modules) {
            module.getConfiguration().addPages();
        }

        applicationContext.setModules(modules);
    }

    /**
     * Scans classpath for modules.
     *
     * @return List of modules ordered by priority (from high priority to low).
     */
    private List<Module> getModules() {
        List<Module> modules = new ArrayList<Module>();

        URLClassLoader loader =
                (URLClassLoader) getClass().getClassLoader();

        URL[] classPath = loader.getURLs();

        for (URL url : classPath) {
            if (Module.isModuleUrl(url)) {
                modules.add(new Module(url));
            }
        }

        return modules;
    }

    void setReloadingClassLoader(ClassLoader reloadingClassLoader) {
        this.reloadingClassLoader = reloadingClassLoader;
    }

    /**
     * Run production mode request handling.
     *
     * @param request  Request.
     * @param response Response.
     * @return Page run result.
     * @throws java.io.IOException when Something wrong with IO.
     */
    @SuppressWarnings({"unchecked"})
    private RunResult runProductionService(HttpServletRequest request, HttpServletResponse response) throws IOException {
        RunResult result = new RunResult();

        String path = request.getServletPath();

        Map<String, String> parameterMap = RequestUtil.getRequestParams(request);
        Page page = pageLoader.loadPage(path, parameterMap);

        if (page == null) {
            result.setProcessChain(true);
            return result;
        }

        Configuration templateEngineConfiguration = templateEngineConfigurationPool.getInstance();

        boolean processChain;
        Throwable pageThrowable = null;

        try {
            page.setTemplateEngineConfiguration(templateEngineConfiguration);
            page.setRequest(request);
            page.setFilterConfig(getFilterConfig());
            page.setResponse(response);

            setupPageRequestListener(page);
            handleBeforeProcessPage(page);

            page.parseTemplate();
            processChain = page.isProcessChain();

            page.getOutputStream().flush();
        } catch (Exception e) {
            pageThrowable = e;
            logger.fatal("Can't process " + request.getRequestURL() + ".", e);
            throw new NocturneException("Can't process " + request.getRequestURL() + ".", e);
        } finally {
            handleAfterProcessPage(page, pageThrowable);
            pageLoader.unloadPage(path, parameterMap, page);
            templateEngineConfigurationPool.release(templateEngineConfiguration);
        }

        result.setProcessChain(processChain);

        return result;
    }

    private void setupPageRequestListener(Object page) throws ClassNotFoundException {
        if (pageRequestListeners == null || applicationContext.isDebug()) {
            pageRequestListeners = new ArrayList<Object>();

            for (String name : applicationContext.getPageRequestListeners()) {
                ClassLoader loader = page.getClass().getClassLoader();
                Class<?> clazz = loader.loadClass(name);
                Object listener = applicationContext.getInjector().getInstance(clazz);
                pageRequestListeners.add(listener);
            }
        }
    }

    private void handleBeforeProcessPage(Object page) {
        for (Object listener : pageRequestListeners) {
            if (applicationContext.isDebug()) {
                try {
                    ReflectionUtil.invoke(listener.getClass(), listener, "beforeProcessPage", page);
                } catch (ReflectionException e) {
                    throw new NocturneException("Can't invoke handleBeforeProcessPage.", e);
                }
            } else {
                ((PageRequestListener) listener).beforeProcessPage((Page) page);
            }
        }
    }

    private void handleAfterProcessPage(Object page, Throwable t) {
        for (Object listener : pageRequestListeners) {
            if (applicationContext.isDebug()) {
                try {
                    ReflectionUtil.invoke(listener.getClass(), listener, "afterProcessPage", page, t);
                } catch (ReflectionException e) {
                    throw new NocturneException("Can't invoke afterProcessPage.", e);
                }
            } else {
                ((PageRequestListener) listener).afterProcessPage((Page) page, t);
            }
        }
    }

    /**
     * @return filterConfig Retuns filter configuration instance.
     */
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
        applicationContext.setReloadingClassLoader(reloadingClassLoader);
        RunResult runResult = new RunResult();

        Object page = null;
        Throwable pageThrowable = null;

        try {
            Class pageLoaderClass = reloadingClassLoader.loadClass(PageLoader.class.getName());
            Object pageLoader = pageLoaderClass.newInstance();

            page = ReflectionUtil.invoke(pageLoader, "loadPage", request.getServletPath(), RequestUtil.getRequestParams(request));
            if (page == null) {
                runResult.setProcessChain(true);
            } else {
                processPage(request, response, page, runResult);
            }
        } catch (Throwable e) {
            pageThrowable = e;
            logger.fatal("Can't process " + request.getRequestURL() + ".", e);
            throw new NocturneException("Can't process " + request.getRequestURL() + ".", e);
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
    private void processPage(HttpServletRequest request, HttpServletResponse response, Object page, RunResult runResult) throws IOException, ClassNotFoundException {
        Configuration templateEngineConfiguration = templateEngineConfigurationPool.getInstance();

        try {
            ReflectionUtil.invoke(page, "setTemplateEngineConfiguration", templateEngineConfiguration);
            ReflectionUtil.invoke(page, "setRequest", request);
            ReflectionUtil.invoke(page, "setResponse", response);
            ReflectionUtil.invoke(page, "setFilterConfig", getFilterConfig());

            setupPageRequestListener(page);
            handleBeforeProcessPage(page);
            ReflectionUtil.invoke(page, "parseTemplate");
            runResult.setProcessChain((Boolean) ReflectionUtil.invoke(page, "isProcessChain"));
        } catch (ReflectionException e) {
            throw new NocturneException("Can't run method via reflection.", e);
        } finally {
            templateEngineConfigurationPool.release(templateEngineConfiguration);
        }
    }

    /**
     * Initializes nocturne application.
     * Reads configuration parameters from web.xml.
     *
     * @param config Config.
     * @throws javax.servlet.ServletException when method fails.
     */
    public void init(FilterConfig config) throws ServletException {
        try {
            templateEngineConfigurationPool
                    = new TemplateEngineConfigurationPool(config);

            filterConfig = config;
            ApplicationContextLoader.run();
            applicationContext.setServletContext(config.getServletContext());

            // Pass application context to servlet
            config.getServletContext().setAttribute("applicationContext", applicationContext);

            initializeModules();

            // Log.
            if (!applicationContext.isDebug()) {
                logger.info("Nocturne RequestDispatcter has been initialized.");
            }
        } catch (Exception e) {
            logger.error("Exception while initialization DispatchFilter.", e);
            throw new ServletException(e);
        }
    }

    /**
     * Handles requests to the application pages.
     *
     * @param request     Request.
     * @param response    Response.
     * @param filterChain Filter chain.
     * @throws ServletException when method fails.
     * @throws IOException      when something wrong with IO.
     */
    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        try {
            //applicationContext.clearComponentsByTemplate();
            applicationContext.setRequestAndResponse(request, response);

            setupHeaders(response);
            RunResult runResult;

            if (applicationContext.isDebug()) {
                runResult = runDebugService(request, response);
            } else {
                runResult = runProductionService(request, response);
            }
            //applicationContext.clearComponentsByTemplate();

            if (runResult.isProcessChain()) {
                filterChain.doFilter(request, response);
            }
        } catch (Exception e) {
            logger.error("Exception while processing request.", e);
            throw new ServletException(e);
        }
    }

    private void setupHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, max-age=0");
        response.setHeader("Expires", "-1");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Connection", "Keep-Alive");
        response.setHeader("Keep-Alive", "600");
        response.setCharacterEncoding("UTF-8");
        response.setContentType("text/html");
    }

    /**
     * Destroy filter.
     */
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
}
