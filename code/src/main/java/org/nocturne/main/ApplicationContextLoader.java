/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.ModuleInitializationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.module.Configuration;
import org.nocturne.module.Module;
import org.nocturne.prometheus.Prometheus;
import org.nocturne.reset.ResetStrategy;
import org.nocturne.reset.annotation.Persist;
import org.nocturne.reset.annotation.Reset;
import org.nocturne.util.StringUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Mike Mirzayanov
 */
class ApplicationContextLoader {
    private static final Logger logger = Logger.getLogger(ApplicationContextLoader.class);

    private static final Properties properties = new Properties();
    private static final Pattern ITEMS_SPLIT_PATTERN = Pattern.compile("\\s*;\\s*");
    private static final Pattern LANGUAGES_SPLIT_PATTERN = Pattern.compile("[,;\\s]+");
    private static final Pattern COUNTRIES_TO_LANGUAGE_PATTERN = Pattern.compile("([A-Z]{2},)*[A-Z]{2}:[a-z]{2}");

    private static void run() {
        setupDebug();
        setupTemplates();

        if (ApplicationContext.getInstance().isDebug()) {
            setupReloadingClassPaths();
            setupClassReloadingPackages();
            setupClassReloadingExceptions();
            setupDebugCaptionsDir();
            setupDebugWebResourcesDir();
        }

        setupPageRequestListeners();
        setupGuiceModuleClassName();
        setupSkipRegex();
        setupRequestRouter();
        setupDefaultLocale();
        setupCaptionsImplClass();
        setupCaptionFilesEncoding();
        setupAllowedLanguages();
        setupCountryToLanguage();
        setupDefaultPageClassName();
        setupContextPath();
        setupResetProperties();
    }

    private static void setupResetProperties() {
        String strategy = properties.getProperty("nocturne.reset.strategy");
        if (StringUtil.isEmpty(strategy)) {
            ApplicationContext.getInstance().setResetStrategy(ResetStrategy.PERSIST);
        } else {
            ApplicationContext.getInstance().setResetStrategy(ResetStrategy.valueOf(strategy));
        }

        String resetAnnotations = properties.getProperty("nocturne.reset.reset-annotations");
        if (StringUtil.isEmpty(resetAnnotations)) {
            ApplicationContext.getInstance().setResetAnnotations(Collections.singletonList(Reset.class.getName()));
        } else {
            String[] annotations = ITEMS_SPLIT_PATTERN.split(resetAnnotations);
            ApplicationContext.getInstance().setResetAnnotations(Arrays.asList(annotations));
        }

        String persistAnnotations = properties.getProperty("nocturne.reset.persist-annotations");
        if (StringUtil.isEmpty(persistAnnotations)) {
            ApplicationContext.getInstance().setPersistAnnotations(Arrays.asList(
                    Persist.class.getName(),
                    Inject.class.getName()
            ));
        } else {
            String[] annotations = ITEMS_SPLIT_PATTERN.split(persistAnnotations);
            ApplicationContext.getInstance().setPersistAnnotations(Arrays.asList(annotations));
        }
    }

    private static void setupContextPath() {
        if (properties.containsKey("nocturne.context-path")) {
            String contextPath = properties.getProperty("nocturne.context-path");
            if (contextPath != null) {
                ApplicationContext.getInstance().setContextPath(contextPath);
            }
        }
    }

    private static void setupDefaultPageClassName() {
        if (properties.containsKey("nocturne.default-page-class-name")) {
            String className = properties.getProperty("nocturne.default-page-class-name");
            if (className != null && !className.isEmpty()) {
                ApplicationContext.getInstance().setDefaultPageClassName(className);
            }
        }
    }

    private static void setupAllowedLanguages() {
        if (properties.containsKey("nocturne.allowed-languages")) {
            String languages = properties.getProperty("nocturne.allowed-languages");
            if (languages != null && !languages.isEmpty()) {
                String[] tokens = LANGUAGES_SPLIT_PATTERN.split(languages);
                List<String> list = new ArrayList<>();
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        if (token.length() != 2) {
                            throw new ConfigurationException("nocturne.allowed-languages should contain the " +
                                    "list of 2-letters language codes separated with comma.");
                        }
                        list.add(token);
                    }
                }
                ApplicationContext.getInstance().setAllowedLanguages(list);
            }
        }
    }

    private static void setupCountryToLanguage() {
        if (properties.containsKey("nocturne.countries-to-language")) {
            String countriesToLanguage = properties.getProperty("nocturne.countries-to-language");
            if (countriesToLanguage != null && !countriesToLanguage.isEmpty()) {
                String[] tokens = ITEMS_SPLIT_PATTERN.split(countriesToLanguage);
                Map<String, String> result = new HashMap<>();
                for (String token : tokens) {
                    if (!token.isEmpty()) {
                        if (!COUNTRIES_TO_LANGUAGE_PATTERN.matcher(token).matches()) {
                            throw new ConfigurationException("nocturne.countries-to-language should have a form like " +
                                    "\"RU,BY:ru;EN,GB,US,CA:en\".");
                        }
                        String[] countriesAndLanguage = token.split(":");
                        String[] countries = countriesAndLanguage[0].split(",");
                        for (String country : countries) {
                            result.put(country, countriesAndLanguage[1]);
                        }
                    }
                }
                ApplicationContext.getInstance().setCountryToLanguage(result);
            }
        }
    }

    private static void setupCaptionFilesEncoding() {
        if (properties.containsKey("nocturne.caption-files-encoding")) {
            String encoding = properties.getProperty("nocturne.caption-files-encoding");
            if (encoding != null && !encoding.isEmpty()) {
                ApplicationContext.getInstance().setCaptionFilesEncoding(encoding);
            }
        }
    }

    private static void setupCaptionsImplClass() {
        if (properties.containsKey("nocturne.captions-impl-class")) {
            String clazz = properties.getProperty("nocturne.captions-impl-class");
            if (clazz != null && !clazz.isEmpty()) {
                ApplicationContext.getInstance().setCaptionsImplClass(clazz);
            }
        }
    }

    private static void setupDebugCaptionsDir() {
        if (properties.containsKey("nocturne.debug-captions-dir")) {
            String dir = properties.getProperty("nocturne.debug-captions-dir");
            if (dir != null && !dir.isEmpty()) {
                if (!new File(dir).isDirectory() && ApplicationContext.getInstance().isDebug()) {
                    throw new ConfigurationException("nocturne.debug-captions-dir property should be a directory.");
                }
                ApplicationContext.getInstance().setDebugCaptionsDir(dir);
            }
        }
    }

    private static void setupDefaultLocale() {
        if (properties.containsKey("nocturne.default-language")) {
            String language = properties.getProperty("nocturne.default-language");
            if (language != null && !language.isEmpty()) {
                if (language.length() != 2) {
                    throw new ConfigurationException("Language is expected to have exactly two letters.");
                }
                ApplicationContext.getInstance().setDefaultLocale(language);
            }
        }
    }

    private static void setupRequestRouter() {
        if (properties.containsKey("nocturne.request-router")) {
            String resolver = properties.getProperty("nocturne.request-router");
            if (resolver == null || resolver.isEmpty()) {
                throw new ConfigurationException("Parameter nocturne.request-router can't be empty.");
            }
            ApplicationContext.getInstance().setRequestRouter(resolver);
        } else {
            throw new ConfigurationException("Missed parameter nocturne.request-router.");
        }
    }

    private static void setupDebugWebResourcesDir() {
        if (properties.containsKey("nocturne.debug-web-resources-dir")) {
            String dir = properties.getProperty("nocturne.debug-web-resources-dir");
            if (dir != null && !dir.trim().isEmpty()) {
                ApplicationContext.getInstance().setDebugWebResourcesDir(dir.trim());
            }
        }
    }

    private static void setupClassReloadingExceptions() {
        List<String> exceptions = new ArrayList<>();
        exceptions.add(ApplicationContext.class.getName());
        exceptions.add(Prometheus.class.getName());
        if (properties.containsKey("nocturne.class-reloading-exceptions")) {
            String exceptionsAsString = properties.getProperty("nocturne.class-reloading-exceptions");
            if (exceptionsAsString != null) {
                exceptions.addAll(listOfNonEmpties(ITEMS_SPLIT_PATTERN.split(exceptionsAsString)));
            }
        }
        ApplicationContext.getInstance().setClassReloadingExceptions(exceptions);
    }

    private static void setupClassReloadingPackages() {
        List<String> packages = new ArrayList<>();
        packages.add("org.nocturne");

        if (properties.containsKey("nocturne.class-reloading-packages")) {
            String packagesAsString = properties.getProperty("nocturne.class-reloading-packages");
            if (packagesAsString != null) {
                packages.addAll(listOfNonEmpties(ITEMS_SPLIT_PATTERN.split(packagesAsString)));
            }
        }
        ApplicationContext.getInstance().setClassReloadingPackages(packages);
    }

    private static void setupSkipRegex() {
        if (properties.containsKey("nocturne.skip-regex")) {
            String regex = properties.getProperty("nocturne.skip-regex");
            if (regex != null && !regex.isEmpty()) {
                try {
                    ApplicationContext.getInstance().setSkipRegex(Pattern.compile(regex));
                } catch (PatternSyntaxException e) {
                    throw new ConfigurationException("Parameter nocturne.skip-regex contains invalid pattern.", e);
                }
            }
        }
    }

    private static void setupGuiceModuleClassName() {
        if (properties.containsKey("nocturne.guice-module-class-name")) {
            String module = properties.getProperty("nocturne.guice-module-class-name");
            if (module != null && !module.isEmpty()) {
                ApplicationContext.getInstance().setGuiceModuleClassName(module);
            }
        }
    }

    private static void setupPageRequestListeners() {
        List<String> listeners = new ArrayList<>();
        if (properties.containsKey("nocturne.page-request-listeners")) {
            String pageRequestListenersAsString = properties.getProperty("nocturne.page-request-listeners");
            if (pageRequestListenersAsString != null) {
                listeners.addAll(listOfNonEmpties(ITEMS_SPLIT_PATTERN.split(pageRequestListenersAsString)));
            }
        }
        ApplicationContext.getInstance().setPageRequestListeners(listeners);
    }

    private static void setupReloadingClassPaths() {
        List<File> reloadingClassPaths = new ArrayList<>();
        if (properties.containsKey("nocturne.reloading-class-paths")) {
            String reloadingClassPathsAsString = properties.getProperty("nocturne.reloading-class-paths");
            if (reloadingClassPathsAsString != null) {
                String[] dirs = ITEMS_SPLIT_PATTERN.split(reloadingClassPathsAsString);
                for (String dir : dirs) {
                    if (dir != null && !dir.isEmpty()) {
                        File file = new File(dir);
                        if (!file.isDirectory() && ApplicationContext.getInstance().isDebug()) {
                            throw new ConfigurationException("Each item in nocturne.reloading-class-paths should be a directory,"
                                    + " but " + file + " is not.");
                        }
                        reloadingClassPaths.add(file);
                    }
                }
            }
        }
        ApplicationContext.getInstance().setReloadingClassPaths(reloadingClassPaths);
    }

    private static void setupTemplates() {
        if (properties.containsKey("nocturne.templates-update-delay")) {
            try {
                int templatesUpdateDelay = Integer.parseInt(properties.getProperty("nocturne.templates-update-delay"));
                if (templatesUpdateDelay < 0 || templatesUpdateDelay > 86400) {
                    throw new ConfigurationException("Parameter nocturne.templates-update-delay should be non-negative integer not greater than 86400.");
                }
                ApplicationContext.getInstance().setTemplatesUpdateDelay(templatesUpdateDelay);
            } catch (NumberFormatException e) {
                throw new ConfigurationException("Parameter nocturne.templates-update-delay should be integer.", e);
            }
        }

        if (properties.containsKey("nocturne.template-paths")) {
            String[] templatePaths = ITEMS_SPLIT_PATTERN.split(StringUtils.trimToEmpty(
                    properties.getProperty("nocturne.template-paths")
            ));

            for (String templatePath : templatePaths) {
                if (templatePath.isEmpty()) {
                    throw new ConfigurationException("Item of parameter nocturne.template-paths can't be empty.");
                }
            }
            ApplicationContext.getInstance().setTemplatePaths(templatePaths);
        } else if (properties.containsKey("nocturne.templates-path")) {
            logger.warn("Property nocturne.templates-path is deprecated. Use semicolon separated nocturne.template-paths.");

            String templatesPath = StringUtils.trimToEmpty(properties.getProperty("nocturne.templates-path"));
            if (templatesPath.isEmpty()) {
                throw new ConfigurationException("Parameter nocturne.templates-path can't be empty.");
            }
            ApplicationContext.getInstance().setTemplatePaths(new String[]{templatesPath});
        } else {
            throw new ConfigurationException("Missing parameter nocturne.template-paths.");
        }

        if (properties.containsKey("nocturne.sticky-template-paths")) {
            String stickyTemplatePaths = StringUtils.trimToEmpty(properties.getProperty("nocturne.sticky-template-paths"));
            if (!stickyTemplatePaths.isEmpty()) {
                ApplicationContext.getInstance().setStickyTemplatePaths(Boolean.parseBoolean(stickyTemplatePaths));
            }
        }

        if (properties.containsKey("nocturne.use-component-templates")) {
            String useComponentTemplates = properties.getProperty("nocturne.use-component-templates");
            if (!"false".equals(useComponentTemplates) && !"true".equals(useComponentTemplates)) {
                throw new ConfigurationException("Parameter nocturne.use-component-templates expected to be 'false' or 'true'.");
            }
            boolean use = "true".equals(useComponentTemplates);
            ApplicationContext.getInstance().setUseComponentTemplates(use);
            if (use && properties.containsKey("nocturne.component-templates-less-commons-file")) {
                String componentTemplatesLessCommonsFileAsString
                        = properties.getProperty("nocturne.component-templates-less-commons-file");
                if (!StringUtil.isBlank(componentTemplatesLessCommonsFileAsString)) {
                    File componentTemplatesLessCommonsFile = new File(componentTemplatesLessCommonsFileAsString);
                    if (componentTemplatesLessCommonsFile.isFile()) {
                        ApplicationContext.getInstance().setComponentTemplatesLessCommonsFile(componentTemplatesLessCommonsFile);
                    } else {
                        throw new ConfigurationException("Parameter nocturne.component-templates-less-commons-file is expected to be a file.");
                    }
                }
            }
        }
    }

    private static void setupDebug() {
        ApplicationContext.getInstance().setDebug(Boolean.parseBoolean(properties.getProperty("nocturne.debug")));
    }

    private static List<String> listOfNonEmpties(String[] strings) {
        List<String> result = new ArrayList<>(strings.length);
        for (String s : strings) {
            if (!StringUtil.isEmpty(s)) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Scans classpath for modules.
     *
     * @return List of modules ordered by priority (from high priority to low).
     */
    private static List<Module> getModulesFromClasspath() {
        List<Module> modules = new ArrayList<>();
        URLClassLoader loader = (URLClassLoader) ApplicationContext.class.getClassLoader();
        URL[] classPath = loader.getURLs();
        for (URL url : classPath) {
            if (Module.isModuleUrl(url)) {
                modules.add(new Module(url));
            }
        }
        return modules;
    }

    /**
     * Runs init() method for all modules.
     * Each module should be initialized on the application startup.
     */
    private static void initializeModules() {
        List<Module> modules = getModulesFromClasspath();

        for (Module module : modules) {
            module.init();
        }

        modules.sort((moduleA, moduleB) -> {
            int priorityComparisonResult = Integer.compare(moduleB.getPriority(), moduleA.getPriority());
            if (priorityComparisonResult != 0) {
                return priorityComparisonResult;
            }

            return moduleA.getName().compareTo(moduleB.getName());
        });

        for (Module module : modules) {
            module.getConfiguration().addPages();
        }

        ApplicationContext.getInstance().setModules(modules);
    }

    private static void setupInjector() {
        String guiceModuleClassName = ApplicationContext.getInstance().getGuiceModuleClassName();
        GenericIocModule module = new GenericIocModule();

        if (!StringUtil.isEmpty(guiceModuleClassName)) {
            try {
                module.setModule(getApplicationModule(guiceModuleClassName));
            } catch (Exception e) {
                throw new ConfigurationException("Can't load application Guice module.", e);
            }
        }

        Injector injector = Guice.createInjector(module);

        if (ApplicationContext.getInstance().isDebug()) {
            try {
                Method method = ApplicationContext.class.getDeclaredMethod("setInjector", Injector.class);
                method.setAccessible(true);
                method.invoke(ApplicationContext.getInstance(), injector);
            } catch (NoSuchMethodException e) {
                throw new NocturneException("Can't find method setInjector.", e);
            } catch (InvocationTargetException e) {
                throw new NocturneException("InvocationTargetException", e);
            } catch (IllegalAccessException e) {
                throw new NocturneException("IllegalAccessException", e);
            }
        } else {
            ApplicationContext.getInstance().setInjector(injector);
        }
    }

    private static com.google.inject.Module getApplicationModule(String guiceModuleClassName) throws Exception {
        Class<?> moduleClass = ApplicationContext.class.getClassLoader().loadClass(guiceModuleClassName);
        AtomicReference<Exception> exception = new AtomicReference<>();

        try {
            return (com.google.inject.Module) moduleClass.getConstructor().newInstance();
        } catch (Exception e) {
            exception.compareAndSet(null, e);
        }

        try {
            Method getInstanceMethod = moduleClass.getMethod("getInstance");
            if (Modifier.isStatic(getInstanceMethod.getModifiers())
                    && com.google.inject.Module.class.isAssignableFrom(getInstanceMethod.getReturnType())) {
                return (com.google.inject.Module) getInstanceMethod.invoke(null);
            }
        } catch (Exception e) {
            exception.compareAndSet(null, e);
        }

        try {
            Method createInstanceMethod = moduleClass.getMethod("createInstance");
            if (Modifier.isStatic(createInstanceMethod.getModifiers())
                    && com.google.inject.Module.class.isAssignableFrom(createInstanceMethod.getReturnType())) {
                return (com.google.inject.Module) createInstanceMethod.invoke(null);
            }
        } catch (Exception e) {
            exception.compareAndSet(null, e);
        }

        try {
            Method newInstanceMethod = moduleClass.getMethod("newInstance");
            if (Modifier.isStatic(newInstanceMethod.getModifiers())
                    && com.google.inject.Module.class.isAssignableFrom(newInstanceMethod.getReturnType())) {
                return (com.google.inject.Module) newInstanceMethod.invoke(null);
            }
        } catch (Exception e) {
            exception.compareAndSet(null, e);
        }

        throw exception.get();
    }

    private static void runModuleStartups() {
        List<Module> modules = ApplicationContext.getInstance().getModules();
        for (Module module : modules) {
            String startupClassName = module.getStartupClassName();
            if (!startupClassName.isEmpty()) {
                Runnable runnable;
                try {
                    runnable = (Runnable) ApplicationContext.getInstance().getInjector().getInstance(
                            ApplicationContext.class.getClassLoader().loadClass(startupClassName));
                } catch (ClassCastException e) {
                    throw new ModuleInitializationException("Startup class " + startupClassName
                            + " must implement Runnable.", e);
                } catch (ClassNotFoundException e) {
                    throw new ModuleInitializationException("Can't load startup class be name "
                            + startupClassName + '.', e);
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        }
    }

    static void initialize() {
        synchronized (ApplicationContextLoader.class) {
            run();
            initializeModules();
            setupInjector();
            runModuleStartups();
            ApplicationContext.getInstance().setInitialized();
        }
    }

    static void shutdown() {
        synchronized (ApplicationContextLoader.class) {
            ApplicationContext.getInstance().getModules().parallelStream()
                    .map(Module::getConfiguration).filter(Objects::nonNull).forEach(Configuration::shutdown);
        }
    }

    static {

        try (InputStream inputStream = ApplicationContextLoader.class.getResourceAsStream(Constants.CONFIGURATION_FILE)) {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new ConfigurationException("Can't load resource file " + Constants.CONFIGURATION_FILE + '.', e);
        }
    }
}
