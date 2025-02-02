/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.google.inject.Injector;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;
import org.nocturne.caption.Captions;
import org.nocturne.caption.CaptionsImpl;
import org.nocturne.collection.SingleEntryList;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.exception.ReflectionException;
import org.nocturne.geoip.GeoIpUtil;
import org.nocturne.link.Link;
import org.nocturne.module.Module;
import org.nocturne.reset.ResetStrategy;
import org.nocturne.util.ReflectionUtil;
import org.nocturne.util.RequestUtil;
import org.nocturne.util.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * This is global singleton object, accessible from all levels of
 * application. Use it to get current page and component.
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class ApplicationContext {
    private static final org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(ApplicationContext.class);

    /**
     * The only singleton instance.
     */
    private static final ApplicationContext INSTANCE = new ApplicationContext();

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * Lock to perform synchronized operations.
     */
    private final Lock lock = new ReentrantLock();

    /**
     * Current page. Stored as ThreadLocal.
     */
    private static final ThreadLocal<Page> currentPage = new ThreadLocal<>();

    /**
     * Current component. Stored as ThreadLocal.
     */
    private static final ThreadLocal<Component> currentComponent = new ThreadLocal<>();

    /**
     * Is in debug mode?
     */
    private boolean debug;

    /**
     * List of directories to be scanned for recompiled classes. Possibly, it depends on your IDE.
     */
    private Set<File> reloadingClassPaths;

    /**
     * Context path of the application.
     * Use {@code null} to use ApplicationContext.getInstance().getRequest().getContextPath().
     */
    private String contextPath;

    /**
     * List of listener class names.
     */
    private Set<String> pageRequestListeners;

    /**
     * Request router class name.
     */
    private String requestRouter;

    /**
     * IoC module class name.
     */
    private String guiceModuleClassName;

    /**
     * List of packages (or classes) which will be reloaded using ReloadingClassLoader.
     */
    private Set<String> classReloadingPackages;

    /**
     * List of packages (or classes) which should not be reloaded using ReloadingClassLoader,
     * even they are in classReloadingPackages.
     */
    private Set<String> classReloadingExceptions;

    /**
     * Where to find templates. Contains relative paths from
     * deployed application root. For example: WEB-INF/templates.
     */
    private String[] templatePaths;

    /**
     * Indicates if template loader should stick to last successful template path
     * or always check template paths in the configured order.
     * Default value is {@code true}.
     */
    private boolean stickyTemplatePaths = true;

    /**
     * Preprocess FTL templates to read as component templates (if <template>...</template> has found).
     */
    private boolean useComponentTemplates;

    /**
     * Autoimported file for all component LESS styles.
     */
    private File componentTemplatesLessCommonsFile;

    /**
     * Servlet context.
     */
    private ServletContext servletContext;

    /**
     * What page to show if RequestRouter returns {@code null}.
     */
    private String defaultPageClassName;

    /**
     * Pattern: if request.getServletPath() (example: /some/path) matches it, request
     * ignored by nocturne.
     */
    private Pattern skipRegex;

    /**
     * Default locale or English by default.
     */
    private Locale defaultLocale = new Locale("en");

    /**
     * Where to find caption property files, used in case of CaptionsImpl used.
     */
    private String debugCaptionsDir;

    /**
     * Where to find resources by DebugResourceFilter.
     */
    private String debugWebResourcesDir;

    /**
     * Class name for Captions implementations.
     */
    private String captionsImplClass = CaptionsImpl.class.getName();

    /**
     * Captions implementation instance.
     */
    private Captions captions;

    /**
     * Encoding for caption property files, used in case of CaptionsImpl used.
     */
    private String captionFilesEncoding = StandardCharsets.UTF_8.name();

    /**
     * Allowed languages (use 2-letter codes). Only English by default.
     */
    private List<String> allowedLanguages = Collections.singletonList("en");

    /**
     * To use geoip to setup language by 2-letter uppercase country code (ISO 3166 code).
     * The property should have a form like: RU,BY:ru;EN,GB,US,CA:en.
     */
    private Map<String, String> countryToLanguage = new HashMap<>();

    /**
     * Default reset strategy for fields of Components: should they be reset after request processing.
     */
    private ResetStrategy resetStrategy;

    /**
     * Delay between checks of template files to be changed (in seconds).
     */
    private int templatesUpdateDelay = 60;

    /**
     * List of annotation classes to override default strategy, should be used on classes or fields.
     */
    private Set<String> resetAnnotations;

    /**
     * List of annotation classes to override default strategy, should be used on classes or fields.
     */
    private Set<String> persistAnnotations;

    /**
     * Guice injector.
     */
    private Injector injector;

    /**
     * RequestContext for current thread.
     */
    private static final ThreadLocal<RequestContext> requestsPerThread = new ThreadLocal<>();

    /**
     * Reloading class loader for current thread, used in debug mode only.
     */
    private static final ThreadLocal<ClassLoader> reloadingClassLoaderPerThread = new ThreadLocal<>();

    /**
     * Current reloading class loader.
     */
    private ClassLoader reloadingClassLoader = getClass().getClassLoader();

    /**
     * List of loaded modules.
     */
    private List<Module> modules = new ArrayList<>();

    /**
     * ApplicationContext is initialized.
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final Lock initializedLock = new ReentrantLock();
    private final Condition initializedCondition = initializedLock.newCondition();

    void setInitialized() {
        initializedLock.lock();
        try {
            if (!initialized.getAndSet(true)) {
                initializedCondition.signalAll();
            }
        } finally {
            initializedLock.unlock();
        }
    }

    @SuppressWarnings("WeakerAccess")
    @Contract(pure = true)
    boolean isInitialized() {
        return initialized.get();
    }

    void setRequestAndResponse(HttpServletRequest request, HttpServletResponse response) {
        requestsPerThread.set(new RequestContext(request, response));
    }

    public void unsetRequestAndResponse() {
        requestsPerThread.set(new RequestContext(null, null));
    }

    /**
     * In debug mode it will return reloading class loader, and it
     * will return typical web-application class loader in production mode.
     *
     * @return Reloading class loader (for debug mode) and usual web-application
     * class loader (for production mode).
     */
    public ClassLoader getReloadingClassLoader() {
        if (debug) {
            return reloadingClassLoaderPerThread.get();
        } else {
            return reloadingClassLoader;
        }
    }

    /**
     * @return Returns application context path.
     * You should build paths in your application by
     * concatenation getContextPath() and relative path inside
     * the application.
     */
    public String getContextPath() {
        if (contextPath == null) {
            return getRequest().getContextPath();
        } else {
            return contextPath;
        }
    }

    /**
     * Where to find captions properties files if
     * naive org.nocturne.caption.CaptionsImpl backed used and
     * debug mode switched on.
     *
     * @return Directory or null in the production mode.
     */
    @Nullable
    public String getDebugCaptionsDir() {
        if (debug) {
            return debugCaptionsDir;
        } else {
            return null;
        }
    }

    /**
     * @return Default application locale, specified by
     * nocturne.default-language. English if no one specified.
     */
    public Locale getDefaultLocale() {
        return defaultLocale;
    }

    /**
     * @return List of allowed languages, use property nocturne.allowed-languages.
     */
    public List<String> getAllowedLanguages() {
        return Collections.unmodifiableList(allowedLanguages);
    }

    /**
     * @return Map to setup language by 2-letter uppercase country code (ISO 3166 code).
     * The property should have a form like: RU,BY:ru;EN,GB,US,CA:en.
     */
    @SuppressWarnings("WeakerAccess")
    public Map<String, String> getCountryToLanguage() {
        return Collections.unmodifiableMap(countryToLanguage);
    }

    /**
     * @return Default reset strategy for fields of Components: should they be reset after request processing.
     */
    public ResetStrategy getResetStrategy() {
        return resetStrategy;
    }

    /**
     * @return Delay between checks of template files to be changed (in seconds).
     */
    public int getTemplatesUpdateDelay() {
        return templatesUpdateDelay;
    }

    /**
     * @return List of annotation classes to override default strategy, should be used on classes or fields.
     */
    public Set<String> getResetAnnotations() {
        return Collections.unmodifiableSet(resetAnnotations);
    }

    /**
     * @return List of annotation classes to override default strategy, should be used on classes or fields.
     */
    public Set<String> getPersistAnnotations() {
        return Collections.unmodifiableSet(persistAnnotations);
    }

    void setTemplatesUpdateDelay(int templatesUpdateDelay) {
        this.templatesUpdateDelay = templatesUpdateDelay;
    }

    void setUseComponentTemplates(boolean useComponentTemplates) {
        this.useComponentTemplates = useComponentTemplates;
    }

    public boolean isUseComponentTemplates() {
        return useComponentTemplates;
    }

    public File getComponentTemplatesLessCommonsFile() {
        return componentTemplatesLessCommonsFile;
    }

    void setComponentTemplatesLessCommonsFile(File componentTemplatesLessCommonsFile) {
        this.componentTemplatesLessCommonsFile = componentTemplatesLessCommonsFile;
    }

    void setResetStrategy(ResetStrategy resetStrategy) {
        this.resetStrategy = resetStrategy;
    }

    void setResetAnnotations(Collection<String> resetAnnotations) {
        this.resetAnnotations = new LinkedHashSet<>(resetAnnotations);
    }

    void setPersistAnnotations(Collection<String> persistAnnotations) {
        this.persistAnnotations = new LinkedHashSet<>(persistAnnotations);
    }

    /**
     * @return What page to show if RequestRouter returns {@code null}.
     * Returns {@code null} if application should return 404 on it.
     */
    public String getDefaultPageClassName() {
        return defaultPageClassName;
    }

    /**
     * @return Encoding for caption files if
     * naive org.nocturne.caption.CaptionsImpl backed used.
     */
    public String getCaptionFilesEncoding() {
        return captionFilesEncoding;
    }

    /**
     * @return Current rendering frame or page.
     */
    public Component getCurrentComponent() {
        return currentComponent.get();
    }

    /**
     * @return Current rendering page instance.
     */
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

    void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    void setLink(Link link) {
        getRequest().setAttribute("nocturne.current-page-link", link);
    }

    /**
     * @return Link annotation instance, which was choosen by LinkedRequestRouter as
     * link for current request.
     */
    public Link getLink() {
        return (Link) getRequest().getAttribute("nocturne.current-page-link");
    }

    void setDefaultPageClassName(String defaultPageClassName) {
        this.defaultPageClassName = defaultPageClassName;
    }

    void setCurrentPage(Page page) {
        currentPage.set(page);
    }

    /**
     * @return Is application in the debug mode?
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * @return Captions implementation class name.
     */
    public String getCaptionsImplClass() {
        return captionsImplClass;
    }

    void setCurrentComponent(Component component) {
        currentComponent.set(component);
    }

    /**
     * @return List of directories to be scanned for recompiled classes.
     * Used in the debug mode only.
     * Possibly, it depends on your IDE.
     * Setup it by nocturne.reloading-class-paths.
     */
    public List<File> getReloadingClassPaths() {
        return new LinkedList<>(reloadingClassPaths);
    }

    /**
     * @return List of listener class names. Setup it by nocturne.page-request-listeners.
     */
    public List<String> getPageRequestListeners() {
        return new LinkedList<>(pageRequestListeners);
    }

    void addRequestOverrideParameter(String name, String value) {
        requestsPerThread.get().addOverrideParameter(name, value);
    }

    void addRequestOverrideParameter(String name, List<String> values) {
        requestsPerThread.get().addOverrideParameter(name, values);
    }

    Map<String, List<String>> getRequestOverrideParameters() {
        return requestsPerThread.get().getOverrideParameters();
    }

    void setDebug(boolean debug) {
        this.debug = debug;
    }

    void setReloadingClassPaths(List<File> reloadingClassPaths) {
        this.reloadingClassPaths = new LinkedHashSet<>(reloadingClassPaths);
    }

    void setPageRequestListeners(List<String> pageRequestListeners) {
        this.pageRequestListeners = new LinkedHashSet<>(pageRequestListeners);
    }

    void setCaptionFilesEncoding(String captionFilesEncoding) {
        this.captionFilesEncoding = captionFilesEncoding;
    }

    void setRequestRouter(String requestRouter) {
        this.requestRouter = requestRouter;
    }

    void setTemplatePaths(String[] templatePaths) {
        this.templatePaths = Arrays.copyOf(templatePaths, templatePaths.length);
    }

    void setDefaultLocale(String defaultLanguage) {
        this.defaultLocale = new Locale(defaultLanguage.toLowerCase());
    }

    void setGuiceModuleClassName(String guiceModuleClassName) {
        this.guiceModuleClassName = guiceModuleClassName;
    }

    void setClassReloadingExceptions(List<String> classReloadingExceptions) {
        this.classReloadingExceptions = new LinkedHashSet<>(classReloadingExceptions);
    }

    /**
     * @return Returns request router instance. Specify nocturne.request-router
     * property to set its class name.
     */
    public String getRequestRouter() {
        return requestRouter;
    }

    /**
     * @return Guice IoC module class name. Set nocturne.guice-module-class-name property.
     */
    public String getGuiceModuleClassName() {
        return guiceModuleClassName;
    }

    void setClassReloadingPackages(List<String> classReloadingPackages) {
        this.classReloadingPackages = new LinkedHashSet<>(classReloadingPackages);
    }

    void setInjector(Injector injector) {
        lock.lock();
        try {
            this.injector = injector;
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return Guice injector. It is not good idea to use it.
     */
    public Injector getInjector() {
        return injector;
    }

    /**
     * @return List of packages (or classes) which will be reloaded using
     * ReloadingClassLoader. Set nocturne.class-reloading-packages to specify it.
     */
    public List<String> getClassReloadingPackages() {
        return new LinkedList<>(classReloadingPackages);
    }

    /**
     * @return List of packages (or classes) which should not be reloaded
     * using ReloadingClassLoader, even they are in classReloadingPackages.
     * Set nocturne.class-reloading-exceptions to specify the value.
     */
    public List<String> getClassReloadingExceptions() {
        return new LinkedList<>(classReloadingExceptions);
    }

    /**
     * @return Where to find templates. Contains relative paths from deployed application root. For example: WEB-INF/templates.
     * Set nocturne.template-paths - semicolon separated list of paths.
     * Set nocturne.templates-path (deprecated) - single path.
     */
    public String[] getTemplatePaths() {
        return Arrays.copyOf(templatePaths, templatePaths.length);
    }

    /**
     * @return Flag that indicates if template loader should stick to last successful template path
     * or always check template paths in the configured order.
     * Default value is {@code true}.
     */
    public boolean isStickyTemplatePaths() {
        return stickyTemplatePaths;
    }

    public void setStickyTemplatePaths(boolean stickyTemplatePaths) {
        this.stickyTemplatePaths = stickyTemplatePaths;
    }

    void setAllowedLanguages(List<String> allowedLanguages) {
        this.allowedLanguages = new ArrayList<>(allowedLanguages);
    }

    void setCountryToLanguage(Map<String, String> countryToLanguage) {
        this.countryToLanguage = new HashMap<>(countryToLanguage);
    }

    void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    void setDebugCaptionsDir(String debugCaptionsDir) {
        this.debugCaptionsDir = debugCaptionsDir;
    }

    /**
     * @return Returns current servlet context.
     */
    public ServletContext getServletContext() {
        return servletContext;
    }


    /**
     * @return if request.getServletPath() (example: /some/path) matches it, request ignored by nocturne.
     * Use nocturne.skip-regex to set it.
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

    public boolean hasRequest() {
        RequestContext requestContext = requestsPerThread.get();
        return requestContext != null && requestContext.getRequest() != null;
    }

    /**
     * @return Returns current servlet request instance.
     */
    public HttpServletRequest getRequest() {
        return requestsPerThread.get().getRequest();
    }

    public boolean hasResponse() {
        RequestContext requestContext = requestsPerThread.get();
        return requestContext != null && requestContext.getResponse() != null;
    }

    /**
     * @return Returns current servlet response instance.
     */
    public HttpServletResponse getResponse() {
        return requestsPerThread.get().getResponse();
    }

    void setReloadingClassLoader(ClassLoader loader) {
        if (debug) {
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
            logger.error("Path " + dir.getName() + " expected to be a directory.");
            throw new ConfigurationException("Path " + dir.getName() + " expected to be a directory.");
        }
        reloadingClassPaths.add(dir);

        if (debug) {
            ReloadingContext context = ReloadingContext.getInstance();
            try {
                ReflectionUtil.invoke(context, "addReloadingClassPath", dir);
            } catch (ReflectionException e) {
                logger.error("Can't call addReloadingClassPath for ReloadingContext.", e);
                throw new NocturneException("Can't call addReloadingClassPath for ReloadingContext.", e);
            }
        } else {
            ReloadingContext.getInstance().addReloadingClassPath(dir);
        }
    }

    public void addClassReloadingException(String packageOrClassName) {
        if (debug) {
            classReloadingExceptions.add(packageOrClassName);
            ReloadingContext.getInstance().addClassReloadingException(packageOrClassName);
        }
    }

    /**
     * @param shortcut Shortcut value.
     * @return Use the method to work with captions from your code.
     * Usually, it is not good idea, because captions are part of view layer.
     */
    @SuppressWarnings("DollarSignInName")
    public String $(String shortcut) {
        return $(shortcut, ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    /**
     * @param shortcut Shortcut value.
     * @param args     Shortcut arguments.
     * @return Use the method to work with captions from your code.
     * Usually, it is not good idea, because captions are part of view layer.
     */
    @SuppressWarnings({"OverloadedVarargsMethod", "DollarSignInName"})
    public String $(String shortcut, Object... args) {
        shortcut = shortcut.trim();
        initializeCaptions();
        return captions.find(shortcut, args);
    }

    /**
     * @param locale   Expected locale.
     * @param shortcut Shortcut value.
     * @param args     Shortcut arguments.
     * @return Use the method to work with captions from your code.
     * Usually, it is not good idea, because captions are part of view layer.
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public String getCaption(Locale locale, String shortcut, Object... args) {
        shortcut = shortcut.trim();
        initializeCaptions();
        return captions.find(locale, shortcut, args);
    }

    /**
     * @param locale   Expected locale.
     * @param shortcut Shortcut value.
     * @return Use the method to work with captions from your code.
     * Usually, it is not good idea, because captions are part of view layer.
     */
    public String getCaption(Locale locale, String shortcut) {
        shortcut = shortcut.trim();
        initializeCaptions();
        return captions.find(locale, shortcut);
    }

    @SuppressWarnings("unchecked")
    private void initializeCaptions() {
        if (captions != null) {
            return;
        }

        lock.lock();
        try {
            Class<? extends Captions> clazz = (Class<? extends Captions>) getClass().getClassLoader().loadClass(captionsImplClass);
            captions = injector.getInstance(clazz);
        } catch (ClassNotFoundException e) {
            logger.error("Class " + captionsImplClass + " not found.", e);
            throw new ConfigurationException("Class " + captionsImplClass + " should implement Captions.", e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * @return Locale for current request.
     */
    public Locale getLocale() {
        return requestsPerThread.get().getLocale();
    }

    /**
     * @return List of loaded modules.
     */
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    void setModules(List<Module> modules) {
        this.modules = new ArrayList<>(modules);
    }

    /**
     * @return Prefix before attributes in request which
     * will be injected as parameters in Components.
     */
    public static String getAdditionalParamsRequestAttributePrefix() {
        return "nocturne.additional-parameter.";
    }

    private static String getActionRequestPageClassName() {
        return "nocturne.request-page-class-name";
    }

    private static String getActionRequestParamName() {
        return "nocturne.request-action";
    }

    void setRequestAction(String action) {
        getRequest().setAttribute(getActionRequestParamName(), action);
    }

    /**
     * @return Action for current request (how request router decided). Empty string if
     * no one specified. Typically, gets from action parameter (example: ?action=test)
     * or link template (example: "user/{action}").
     */
    public String getRequestAction() {
        return (String) getRequest().getAttribute(getActionRequestParamName());
    }

    void setRequestPageClassName(String pageClassName) {
        getRequest().setAttribute(getActionRequestPageClassName(), pageClassName);
    }

    /**
     * @return Page class name for current request (how request router decided).
     */
    public String getRequestPageClassName() {
        return (String) getRequest().getAttribute(getActionRequestPageClassName());
    }

    public void setDebugWebResourcesDir(String debugWebResourcesDir) {
        this.debugWebResourcesDir = debugWebResourcesDir;
    }

    /**
     * @return Returns the directory where to find resources by DebugResourceFilter.
     */
    public String getDebugWebResourcesDir() {
        return debugWebResourcesDir;
    }

    /**
     * @param runnable Runnable to be executed after ApplicationContext has been initialized.
     */
    public void executeAfterInitialization(Runnable runnable) {
        new Thread(() -> {
            initializedLock.lock();
            try {
                while (!isInitialized()) {
                    try {
                        initializedCondition.await();
                    } catch (InterruptedException ignored) {
                        // No operations.
                    }
                }
                runnable.run();
            } finally {
                initializedLock.unlock();
            }
        }).start();
    }

    /**
     * Stores current request context: request, response and locale.
     */
    private static final class RequestContext {
        private static final Pattern ACCEPT_LANGUAGE_SPLIT_PATTERN = Pattern.compile("[,;-]");
        private static final String LANGUAGE_COOKIE_NAME = "nocturne.language";
        /**
         * Http servlet request.
         */
        private final HttpServletRequest request;

        /**
         * Http servlet response.
         */
        private final HttpServletResponse response;

        /**
         * Locale for current request.
         */
        private Locale locale;

        /**
         * Parameters which override request params.
         */
        private Map<String, List<String>> overrideParameters;

        private RequestContext(@Nullable HttpServletRequest request, @Nullable HttpServletResponse response) {
            if ((request == null) ^ (response == null)) {
                logger.error("It is not possible case '(request == null) ^ (response == null)'.");
                throw new IllegalArgumentException("It is not possible case '(request == null) ^ (response == null)'.");
            }

            this.request = request;
            this.response = response;

            if (request == null) {
                this.locale = null;
            } else {
                setupLocale();
            }
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
         * @return Locale for current request.
         * Uses lang, language, locale parameters to find
         * current locale: 2-letter language code.
         * If it specified once, stores current locale in the session.
         */
        public Locale getLocale() {
            return locale;
        }

        @Contract(value = "null -> true", pure = true)
        private static boolean isInvalidLanguage(@Nullable String lang) {
            return lang == null || lang.length() != 2;
        }

        private void setupLocale() {
            Map<String, List<String>> requestMap = RequestUtil.getRequestParams(request);

            String lang = RequestUtil.getFirst(requestMap, "lang");

            if (isInvalidLanguage(lang)) {
                lang = RequestUtil.getFirst(requestMap, "language");
            }

            if (isInvalidLanguage(lang)) {
                lang = RequestUtil.getFirst(requestMap, "locale");
            }

            if (isInvalidLanguage(lang)) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    lang = (String) session.getAttribute("nocturne.language");
                }

                if (isInvalidLanguage(lang)) {
                    lang = getCookie(LANGUAGE_COOKIE_NAME);
                }

                if (isInvalidLanguage(lang)) {
                    lang = getLanguageByGeoIp();
                    if (isInvalidLanguage(lang)) {
                        String[] languages = getAcceptLanguages();
                        for (String language : languages) {
                            if (getInstance().getAllowedLanguages().contains(language)
                                    && !"en".equalsIgnoreCase(language)) {
                                lang = language;
                                break;
                            }
                        }

                        if (isInvalidLanguage(lang)) {
                            for (String language : languages) {
                                if (getInstance().getAllowedLanguages().contains(language)) {
                                    lang = language;
                                    break;
                                }
                            }
                        }
                    }
                }
                locale = localeByLanguage(lang);
            } else {
                locale = localeByLanguage(lang);
                request.getSession().setAttribute("nocturne.language", locale.getLanguage());
                addCookie(LANGUAGE_COOKIE_NAME, lang, TimeUnit.DAYS.toSeconds(30));
            }
        }

        @SuppressWarnings("SameParameterValue")
        @Nullable
        private String getCookie(String cookieName) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookieName.equals(cookie.getName())) {
                        return cookie.getValue();
                    }
                }
            }
            return null;
        }

        @SuppressWarnings("SameParameterValue")
        private void addCookie(String cookieName, String cookieValue, long maxAge) {
            boolean updated = false;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookie.getName().equals(cookieName)) {
                        cookie.setValue(cookieValue);
                        if (updated) {
                            cookie.setMaxAge(0);
                        } else {
                            cookie.setMaxAge(Ints.checkedCast(maxAge));
                        }
                        cookie.setPath("/");
                        response.addCookie(cookie);
                        updated = true;
                    }
                }
            }

            if (!updated) {
                Cookie cookie = new Cookie(cookieName, cookieValue);
                cookie.setMaxAge(Ints.checkedCast(maxAge));
                cookie.setPath("/");
                response.addCookie(cookie);
            }
        }

        @Nullable
        private String getLanguageByGeoIp() {
            String countryCode = null; // GeoIpUtil.getCountryCode(request);
            String lang = getInstance().getCountryToLanguage().get(countryCode);
            String[] languages = getAcceptLanguages();

            if (ArrayUtils.indexOf(languages, lang) >= 0 && getInstance().getAllowedLanguages().contains(lang)) {
                return lang;
            }

            return null;
        }

        private String[] getAcceptLanguages() {
            String header = request.getHeader("Accept-Language");

            if (StringUtil.isEmpty(header)) {
                return EMPTY_STRING_ARRAY;
            } else {
                String[] result = ACCEPT_LANGUAGE_SPLIT_PATTERN.split(header);
                for (int i = 0; i < result.length; ++i) {
                    result[i] = result[i].toLowerCase();
                }
                return result;
            }
        }

        @Nonnull
        private static Locale localeByLanguage(@Nullable String language) {
            if (getInstance().getAllowedLanguages().contains(language)) {
                return new Locale(Preconditions.checkNotNull(language));
            } else {
                return getInstance().getDefaultLocale();
            }
        }

        private void addOverrideParameter(String name, String value) {
            if (overrideParameters == null) {
                overrideParameters = new HashMap<>();
            }

            overrideParameters.put(name, new SingleEntryList<>(value));
        }

        private void addOverrideParameter(String name, Collection<String> values) {
            if (overrideParameters == null) {
                overrideParameters = new HashMap<>();
            }

            overrideParameters.put(name, new ArrayList<>(values));
        }

        private Map<String, List<String>> getOverrideParameters() {
            return overrideParameters;
        }
    }
}
