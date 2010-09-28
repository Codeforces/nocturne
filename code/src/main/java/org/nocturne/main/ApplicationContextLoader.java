/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import org.nocturne.exception.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** @author Mike Mirzayanov */
class ApplicationContextLoader {
    public static final String CONFIGURATION_FILE = "/nocturne.properties";
    private static final Properties properties = new Properties();

    static void run() {
        setupDebug();
        setupTemplatesPath();
        setupReloadingClassPaths();
        setupPageRequestListeners();
        setupGuiceModuleClassName();
        setupSkipRegex();
        setupClassReloadingPackages();
        setupClassReloadingExceptions();
        setupRequestRouter();
        setupDefaultLocale();
        setupDebugCaptionsDir();
        setupCaptionsImplClass();
        setupCaptionFilesEncoding();
        setupAllowedLanguages();
        setupDefaultPageClassName();
        setupContextPath();
        setupDebugWebResourcesDir();
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
                String[] tokens = languages.split("[,;\\s]+");
                List<String> list = new ArrayList<String>();
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
                if (!(new File(dir).isDirectory()) && ApplicationContext.getInstance().isDebug()) {
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
            if (resolver == null || resolver.length() == 0) {
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
            if (dir != null && dir.trim().length() > 0) {
                ApplicationContext.getInstance().setDebugWebResourcesDir(dir.trim());
            }
        }
    }

    private static void setupClassReloadingExceptions() {
        List<String> exceptions = new ArrayList<String>();
        exceptions.add(ApplicationContext.class.getName());

        if (properties.containsKey("nocturne.class-reloading-exceptions")) {
            String exceptionsAsString = properties.getProperty("nocturne.class-reloading-exceptions");
            if (exceptionsAsString != null) {
                String[] candidats = exceptionsAsString.split("\\s*;\\s*");
                for (String item : candidats) {
                    if (!item.isEmpty()) {
                        exceptions.add(item);
                    }
                }
            }
        }
        ApplicationContext.getInstance().setClassReloadingExceptions(exceptions);
    }

    private static void setupClassReloadingPackages() {
        List<String> packages = new ArrayList<String>();
        packages.add("org.nocturne");

//        packages.add(PageLoader.class.getName());
//        packages.add(PagePool.class.getName());
//        packages.add(Links.class.getName());
//        packages.add(LinkDirective.class.getName());
//        packages.add(LinkedRequestRouter.class.getName());

        if (properties.containsKey("nocturne.class-reloading-packages")) {
            String packagesAsString = properties.getProperty("nocturne.class-reloading-packages");
            if (packagesAsString != null) {
                String[] candidats = packagesAsString.split("\\s*;\\s*");
                for (String item : candidats) {
                    if (!item.isEmpty()) {
                        packages.add(item);
                    }
                }
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
                    throw new ConfigurationException("Parameter nocturne.skip-regex contains invalid pattern.");
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
        List<String> listeners = new ArrayList<String>();
        if (properties.containsKey("nocturne.page-request-listeners")) {
            String pageRequestListenersAsString = properties.getProperty("nocturne.page-request-listeners");
            if (pageRequestListenersAsString != null) {
                String[] candidats = pageRequestListenersAsString.split("\\s*;\\s*");
                for (String listener : candidats) {
                    if (!listener.isEmpty()) {
                        listeners.add(listener);
                    }
                }
            }
        }
        ApplicationContext.getInstance().setPageRequestListeners(listeners);
    }

    private static void setupReloadingClassPaths() {
        List<File> reloadingClassPaths = new ArrayList<File>();
        if (properties.containsKey("nocturne.reloading-class-paths")) {
            String reloadingClassPathsAsString = properties.getProperty("nocturne.reloading-class-paths");
            if (reloadingClassPathsAsString != null) {
                String[] dirs = reloadingClassPathsAsString.split("\\s*;\\s*");
                for (String dir : dirs) {
                    if (!dir.isEmpty()) {
                        File file = new File(dir);
                        if (!file.isDirectory() && ApplicationContext.getInstance().isDebug()) {
                            throw new ConfigurationException("Each item in nocturne.reloading-class-paths should be a directory.");
                        }
                        reloadingClassPaths.add(file);
                    }
                }
            }
        }
        ApplicationContext.getInstance().setReloadingClassPaths(reloadingClassPaths);
    }

    private static void setupTemplatesPath() {
        if (properties.containsKey("nocturne.templates-path")) {
            String templatesPath = properties.getProperty("nocturne.templates-path");
            if (templatesPath == null || templatesPath.length() == 0) {
                throw new ConfigurationException("Parameter nocturne.templates-path can't be empty.");
            }
            ApplicationContext.getInstance().setTemplatesPath(templatesPath);
        } else {
            throw new ConfigurationException("Missed parameter nocturne.templates-path.");
        }
    }

    private static void setupDebug() {
        boolean debug = false;

        if (properties.containsKey("nocturne.debug")) {
            try {
                debug = Boolean.valueOf(properties.getProperty("nocturne.debug"));
            } catch (NullPointerException e) {
                throw new ConfigurationException("Can't cast nocturne.debug to boolean.");
            }
        }

        ApplicationContext.getInstance().setDebug(debug);
    }

    static {
        InputStream inputStream = ApplicationContextLoader.class.getResourceAsStream(CONFIGURATION_FILE);

        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new ConfigurationException("Can't load resource file " + CONFIGURATION_FILE + ".", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    // No operations.
                }
            }
        }
    }
}
