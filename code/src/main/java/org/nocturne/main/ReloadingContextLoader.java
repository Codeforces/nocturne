/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import org.apache.log4j.Logger;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.prometheus.Prometheus;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * @author Mike Mirzayanov
 */
class ReloadingContextLoader {
    private static final Logger logger = Logger.getLogger(ReloadingContextLoader.class);

    private static final Properties properties = new Properties();

    static void run() {
        setupDebug();
        setupTemplates();
        setupSkipRegex();

        if (ReloadingContext.getInstance().isDebug()) {
            setupReloadingClassPaths();
            setupClassReloadingPackages();
            setupClassReloadingExceptions();
        }
    }

    private static void setupTemplates() {
        if (properties.containsKey("nocturne.templates-update-delay")) {
            try {
                int templatesUpdateDelay = Integer.parseInt(properties.getProperty("nocturne.templates-update-delay"));
                if (templatesUpdateDelay < 0 || templatesUpdateDelay > 86400) {
                    logger.error("Parameter nocturne.templates-update-delay should be non-negative integer not greater than 86400.");
                    throw new ConfigurationException("Parameter nocturne.templates-update-delay should be non-negative integer not greater than 86400.");
                }
                ReloadingContext.getInstance().setTemplatesUpdateDelay(templatesUpdateDelay);
            } catch (NumberFormatException e) {
                logger.error("Parameter nocturne.templates-update-delay should be integer.");
                throw new ConfigurationException("Parameter nocturne.templates-update-delay should be integer.");
            }
        }
    }

    private static void setupClassReloadingExceptions() {
        List<String> exceptions = new ArrayList<>();
        exceptions.add(ReloadingContext.class.getName());
        exceptions.add(Prometheus.class.getName());
        if (properties.containsKey("nocturne.class-reloading-exceptions")) {
            String exceptionsAsString = properties.getProperty("nocturne.class-reloading-exceptions");
            if (exceptionsAsString != null) {
                String[] candidates = exceptionsAsString.split("\\s*;\\s*");
                for (String item : candidates) {
                    if (!item.isEmpty()) {
                        exceptions.add(item);
                    }
                }
            }
        }
        ReloadingContext.getInstance().setClassReloadingExceptions(exceptions);
    }

    private static void setupSkipRegex() {
        if (properties.containsKey("nocturne.skip-regex")) {
            String regex = properties.getProperty("nocturne.skip-regex");
            if (regex != null && !regex.isEmpty()) {
                try {
                    ReloadingContext.getInstance().setSkipRegex(Pattern.compile(regex));
                } catch (PatternSyntaxException e) {
                    logger.error("Parameter nocturne.skip-regex contains invalid pattern.");
                    throw new ConfigurationException("Parameter nocturne.skip-regex contains invalid pattern.");
                }
            }
        }
    }

    private static void setupClassReloadingPackages() {
        List<String> packages = new ArrayList<>();
        packages.add("org.nocturne");
        if (properties.containsKey("nocturne.class-reloading-packages")) {
            String packagesAsString = properties.getProperty("nocturne.class-reloading-packages");
            if (packagesAsString != null) {
                String[] candidates = packagesAsString.split("\\s*;\\s*");
                for (String item : candidates) {
                    if (!item.isEmpty()) {
                        packages.add(item);
                    }
                }
            }
        }
        ReloadingContext.getInstance().setClassReloadingPackages(packages);
    }

    private static void setupReloadingClassPaths() {
        List<File> reloadingClassPaths = new ArrayList<>();
        if (properties.containsKey("nocturne.reloading-class-paths")) {
            String reloadingClassPathsAsString = properties.getProperty("nocturne.reloading-class-paths");
            if (reloadingClassPathsAsString != null) {
                String[] dirs = reloadingClassPathsAsString.split("\\s*;\\s*");
                for (String dir : dirs) {
                    if (!dir.isEmpty()) {
                        File file = new File(dir);
                        if (!file.isDirectory() && ReloadingContext.getInstance().isDebug()) {
                            logger.error("Each item in nocturne.reloading-class-paths should be a directory,"
                                         + " but '" + file + "' isn't.");
                            throw new ConfigurationException("Each item in nocturne.reloading-class-paths should be a directory,"
                                                             + " but '" + file + "' isn't.");
                        }
                        reloadingClassPaths.add(file);
                    }
                }
            }
        }
        ReloadingContext.getInstance().setReloadingClassPaths(reloadingClassPaths);
    }

    private static void setupDebug() {
        boolean debug = false;

        if (properties.containsKey("nocturne.debug")) {
            try {
                debug = Boolean.parseBoolean(properties.getProperty("nocturne.debug"));
            } catch (NullPointerException e) {
                logger.error("Can't cast nocturne.debug to boolean.");
                throw new ConfigurationException("Can't cast nocturne.debug to boolean.");
            }
        }

        ReloadingContext.getInstance().setDebug(debug);
    }

    static {

        try (InputStream inputStream = ApplicationContextLoader.class.getResourceAsStream(Constants.CONFIGURATION_FILE)) {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Can't load resource file " + Constants.CONFIGURATION_FILE + '.', e);
            throw new ConfigurationException("Can't load resource file " + Constants.CONFIGURATION_FILE + '.', e);
        }
    }
}
