/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import com.google.inject.Injector;
import org.nocturne.caption.Captions;
import org.nocturne.caption.CaptionsImpl;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.exception.ReflectionException;
import org.nocturne.module.Module;
import org.nocturne.util.ReflectionUtil;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * This is global singleton object, accessible from all levels of
 * application. Use it to get current page and component.
 *
 * @author Mike Mirzayanov
 */
public class ApplicationContext {
    /** The only singleton instance. */
    private static final ApplicationContext INSTANCE =
            new ApplicationContext();

    /** Current page. Stored as ThreadLocal. */
    private ThreadLocal<Page> currentPage = new ThreadLocal<Page>();

    /** Current component. Stored as ThreadLocal. */
    private ThreadLocal<Component> currentComponent = new ThreadLocal<Component>();

    /** Is in debug mode? */
    private boolean debug;

    /** List of directories to be scanned for recompiled classes. Possibly, it depends on your IDE. */
    private List<File> reloadingClassPaths;

    /** List of listener class names. */
    private List<String> pageRequestListeners;

    /** Request routern class name. */
    private String requestRouter;

    /** IoC module class name. */
    private String guiceModuleClassName;

    /** List of packages (or classes) which will be reloaded using ReloadingClassLoader. */
    private List<String> classReloadingPackages;

    /**
     * List of packages (or classes) which should not be reloaded using ReloadingClassLoader,
     * even they are in classReloadingPackages.
     */
    private List<String> classReloadingExceptions;

    /**
     * Where to find templates. Contains relative path from
     * deployed application root. For example: WEB-INF/templates.
     */
    private String templatesPath;

    /** Servlet context. */
    private ServletContext servletContext;

    /**
     * Pattern: if request.getServletPath() (example: /some/path) matches it, request
     * ignored by nocturne.
     */
    private Pattern skipRegex;

    /** Default locale or English by default. */
    private Locale defaultLocale = new Locale("en");

    /** Where to find caption property files, used in case of CaptionsImpl used. */
    private String debugCaptionsDir;

    /** Class name for Captions implementations. */
    private String captionsImplClass = CaptionsImpl.class.getName();

    /** Captions implementation instance. */
    private Captions captions;

    /** Encoding for caption property files, used in case of CaptionsImpl used. */
    private String captionFilesEncoding = "UTF-8";

    /** Allowed languages (use 2-letter codes). Only English by default. */
    private List<String> allowedLanguages = Arrays.asList("en");

    /** Guice injector for production mode. */
    private Injector injectorForProduction;

    /** Guice injector for debug mode. */
    private ThreadLocal<Injector> injector = new ThreadLocal<Injector>();

    /** RequestContext for current thread. */
    private ThreadLocal<RequestContext> requestsPerThread = new ThreadLocal<RequestContext>();

    /** Reloading class loader for current thread, used in debug mode only. */
    private ThreadLocal<ClassLoader> reloadingClassLoaderPerThread = new ThreadLocal<ClassLoader>();

    /** Current reloading class loader. */
    private ClassLoader reloadingClassLoader = getClass().getClassLoader();

    /** List of loaded modules. */
    private List<Module> modules = new ArrayList<Module>();

    void setRequestAndResponse(HttpServletRequest request, HttpServletResponse response) {
        requestsPerThread.set(new RequestContext(request, response));
    }

    /**
     * In debug mode it will return reloading class loader, and it
     * will return typical webapplication class loader in production mode.
     *
     * @return Reloading class loader (for debug mode) and usual webbaplication
     *         class loader (for production mode).
     */
    public ClassLoader getReloadingClassLoader() {
        if (isDebug()) {
            return reloadingClassLoaderPerThread.get();
        } else {
            return reloadingClassLoader;
        }
    }

    /**
     * Where to find captions properties files if
     * naive org.nocturne.caption.CaptionsImpl backed used and
     * debug mode switched on.
     *
     * @return Directory or null in the production mode.
     */
    public String getDebugCaptionsDir() {
        if (isDebug()) {
            return debugCaptionsDir;
        } else {
            return null;
        }
    }

    /**
     * @return Default application locale, specified by
     *         nocturne.default-language. English if no one specified.
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /** @return List of allowed languages, use property nocturne.allowed-languages. */
    public List<String> getAllowedLanguages() {
        return allowedLanguages;
    }

    /**
     * @return Encoding for caption files if
     *         naive org.nocturne.caption.CaptionsImpl backed used.
     */
    public String getCaptionFilesEncoding() {
        return captionFilesEncoding;
    }

    /** @return Current rendering frame or page. */
    public Component getCurrentComponent() {
        return currentComponent.get();
    }

    /** @return Current rendering page instance. */
    public Page getCurrentPage() {
        return currentPage.get();
    }

    /**
     * Method to get application context.
     *
     * @return The only application context instance.
     */
    public static ApplicationContext getInstance() {
        return INSTANCE;
    }

    void setCurrentPage(Page page) {
        currentPage.set(page);
    }

    /** @return Is application in the debug mode? */
    public boolean isDebug() {
        return debug;
    }

    /** @return Captions implementation class name. */
    public String getCaptionsImplClass() {
        return captionsImplClass;
    }

//    void setComponentByTemplate(Template template, Component component) {
//        componentsByTemplate.get().put(template, component);
//    }

    //

    void setCurrentComponent(Component component) {
        currentComponent.set(component);
    }

    /**
     * @return List of directories to be scanned for recompiled classes.
     *         Used in the debug mode only.
     *         Possibly, it depends on your IDE.
     *         Setup it by nocturne.reloading-class-paths.
     */
    public List<File> getReloadingClassPaths() {
        return reloadingClassPaths;
    }

    /** @return List of listener class names. Setup it by nocturne.page-request-listeners. */
    public List<String> getPageRequestListeners() {
        return pageRequestListeners;
    }

    void setDebug(boolean debug) {
        this.debug = debug;
    }

    void setReloadingClassPaths(List<File> reloadingClassPaths) {
        this.reloadingClassPaths = reloadingClassPaths;
    }

    void setPageRequestListeners(List<String> pageRequestListeners) {
        this.pageRequestListeners = pageRequestListeners;
    }

    void setCaptionFilesEncoding(String captionFilesEncoding) {
        this.captionFilesEncoding = captionFilesEncoding;
    }

    void setRequestRouter(String requestRouter) {
        this.requestRouter = requestRouter;
    }

    void setTemplatesPath(String templatesPath) {
        this.templatesPath = templatesPath;
    }

    void setDefaultLocale(String defaultLanguage) {
        this.defaultLocale = new Locale(defaultLanguage.toLowerCase());
    }

    void setGuiceModuleClassName(String guiceModuleClassName) {
        this.guiceModuleClassName = guiceModuleClassName;
    }

    void setClassReloadingExceptions(List<String> classReloadingExceptions) {
        this.classReloadingExceptions = classReloadingExceptions;
    }

    /**
     * @return Returns request router instance. Specify nocturne.request-router
     *         property to set its class name.
     */
    public String getRequestRouter() {
        return requestRouter;
    }

    /** @return Guice IoC module class name. Set nocturne.guice-module-class-name property. */
    public String getGuiceModuleClassName() {
        return guiceModuleClassName;
    }

    void setClassReloadingPackages(List<String> classReloadingPackages) {
        this.classReloadingPackages = classReloadingPackages;
    }

    void setInjector(Injector injector) {
        if (isDebug()) {
            this.injector.set(injector);
        } else {
            synchronized (this) {
                injectorForProduction = injector;
            }
        }
    }

    /** @return Guice injector. It is not good idea to use it. */
    public Injector getInjector() {
        if (isDebug()) {
            return injector.get();
        } else {
            return injectorForProduction;
        }
    }

    /**
     * @return List of packages (or classes) which will be reloaded using
     *         ReloadingClassLoader. Set nocturne.class-reloading-packages to specify it.
     */
    public List<String> getClassReloadingPackages() {
        return classReloadingPackages;
    }

    /**
     * @return List of packages (or classes) which should not be reloaded
     *         using ReloadingClassLoader, even they are in classReloadingPackages.
     *         Set nocturne.class-reloading-exceptions to specify the value.
     */
    public List<String> getClassReloadingExceptions() {
        return classReloadingExceptions;
    }

    /**
     * @return Where to find templates. Contains relative path from deployed application root. For example: WEB-INF/templates.
     *         Set nocturne.templates-path.
     */
    public String getTemplatesPath() {
        return templatesPath;
    }

    void setAllowedLanguages(List<String> allowedLanguages) {
        this.allowedLanguages = allowedLanguages;
    }

    void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    void setDebugCaptionsDir(String debugCaptionsDir) {
        this.debugCaptionsDir = debugCaptionsDir;
    }

    /** Returns current servlet context. */
    public ServletContext getServletContext() {
        return servletContext;
    }


    /**
     * @return if request.getServletPath() (example: /some/path) matches it, request ignored by nocturne.
     *         Use nocturne.skip-regex to set it.
     */
    public Pattern getSkipRegex() {
        return skipRegex;
    }

    void setCaptionsImplClass(String captionsImplClass) {
        this.captionsImplClass = captionsImplClass;
    }

    void setSkipRegex(Pattern skipRegex) {
        this.skipRegex = skipRegex;
    }

//    void clearComponentsByTemplate() {
//        componentsByTemplate.get().clear();
//    }

    //

    /** @return Returns current servlet request instance. */
    public HttpServletRequest getRequest() {
        return requestsPerThread.get().getRequest();
    }

    void setReloadingClassLoader(ClassLoader loader) {
        if (isDebug()) {
            reloadingClassLoaderPerThread.set(loader);
        } else {
            reloadingClassLoader = loader;
        }
    }

    /**
     * Modules use the method to update reloading class path.
     * Do not use it from your code.
     *
     * @param dir Directory to be added.
     */
    public void addReloadingClassPath(File dir) {
        if (!dir.isDirectory()) {
            throw new ConfigurationException("Path " + dir.getName() + " exected to be a directory.");
        }
        reloadingClassPaths.add(dir);

        if (!isDebug()) {
            ReloadingContext.getInstance().addReloadingClassPath(dir);
        } else {
            ReloadingContext context = ReloadingContext.getInstance();
            try {
                ReflectionUtil.invoke(context, "addReloadingClassPath", dir);
            } catch (ReflectionException e) {
                throw new NocturneException("Can't call addReloadingClassPath for ReloadingContext.", e);
            }
        }
    }

    /**
     * @param shortcut Shortcut value.
     * @return Use the method to work with captions from your code.
     *         Usually, it is not good idea, because captions are part of view layer.
     */
    public String $(String shortcut) {
        return $(shortcut, new Object[]{});
    }

    /**
     * @param shortcut Shortcut value.
     * @param args     Shortcut arguments.
     * @return Use the method to work with captions from your code.
     *         Usually, it is not good idea, because captions are part of view layer.
     */
    @SuppressWarnings({"unchecked"})
    public String $(String shortcut, Object... args) {
        shortcut = shortcut.trim();
        
        if (captions == null) {
            synchronized (this) {
                try {
                    Class<? extends Captions> clazz = (Class<? extends Captions>) getClass().getClassLoader().loadClass(getCaptionsImplClass());
                    captions = getInjector().getInstance(clazz);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Class " + getCaptionsImplClass() + " should implement Captions.", e);
                }
            }
        }

        return captions.find(shortcut, args);
    }

    /** @return Locale for current request. */
    public Locale getLocale() {
        return requestsPerThread.get().getLocale();
    }

    /** @return List of loaded modules. */
    public List<Module> getModules() {
        return modules;
    }

    void setModules(List<Module> modules) {
        this.modules = modules;
    }

    /**
     * @return Prefix before attributes in request which
     *         will be injected as parameters in Components.
     */
    public String getAdditionalParamsRequestAttributePrefix() {
        return "nocturne.additional-parameter.";
    }

    private String getActionRequestPageClassName() {
        return "nocturne.request-page-class-name";
    }

    private String getActionRequestParamName() {
        return "nocturne.request-action";
    }

    void setRequestAction(String action) {
        getRequest().setAttribute(getActionRequestParamName(), action);
    }

    /**
     * @return Action for current request (how request router decided). Empty string if
     *         no one specified. Typically, gets from action parameter (example: ?action=test)
     *         or link template (example: "user/{action}").
     */
    public String getRequestAction() {
        return (String) getRequest().getAttribute(getActionRequestParamName());
    }

    void setRequestPageClassName(String pageClassName) {
        getRequest().setAttribute(getActionRequestPageClassName(), pageClassName);
    }

    /** @return Page class name for current request (how request router decided). */
    public String getRequestPageClassName() {
        return (String) getRequest().getAttribute(getActionRequestPageClassName());
    }

    /** Stores current request context: request, response and locale. */
    private static final class RequestContext {
        /** Http servlet request. */
        private final HttpServletRequest request;

        /** Http servlet response. */
        private final HttpServletResponse response;

        /** Locale for current request. */
        private Locale locale;

        private RequestContext(HttpServletRequest request, HttpServletResponse response) {
            this.request = request;
            this.response = response;

            setupLocale();
        }

        /** @return Http servlet request. */
        public HttpServletRequest getRequest() {
            return request;
        }

        /** @return Http servlet response. */
        public HttpServletResponse getResponse() {
            return response;
        }

        /**
         * @return Locale for current request.
         *         Uses lang, language, locale parameters to find
         *         current locale: 2-letter language code.
         *         If it specified once, stores current locale in the session.
         */
        public Locale getLocale() {
            return locale;
        }

        private void setupLocale() {
            String lang = request.getParameter("lang");

            if (lang == null || lang.length() != 2) {
                lang = request.getParameter("language");
            }

            if (lang == null || lang.length() != 2) {
                lang = request.getParameter("locale");
            }

            if (lang == null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                lang = (String) session.getAttribute("nocturne.language");
                }
                locale = localeByLanguage(lang);
            } else {
                locale = localeByLanguage(lang);
                request.getSession().setAttribute("nocturne.language", locale.getLanguage());
            }
        }

        private Locale localeByLanguage(String language) {
            if (ApplicationContext.getInstance().getAllowedLanguages().contains(language)) {
                return new Locale(language);
            } else {
                return ApplicationContext.getInstance().getDefaultLocale();
            }
        }
    }
}
