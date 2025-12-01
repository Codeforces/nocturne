/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import org.apache.log4j.Logger;
import org.nocturne.exception.ConfigurationException;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * @author Mike Mirzayanov
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ReloadingContext {
    private static final Logger logger = Logger.getLogger(ReloadingContext.class);

    private static final ReloadingContext INSTANCE = new ReloadingContext();
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private volatile boolean running = true;
    private boolean debug;
    private Pattern skipRegex;

    private List<File> reloadingClassPaths;
    private List<String> classReloadingPackages;
    private List<String> classReloadingExceptions;

    private int templatesUpdateDelay = 60;

    private ReloadingContext() {
    }

    public static ReloadingContext getInstance() {
        if (!initialized.getAndSet(true)) {
            ReloadingContextLoader.run();
        }

        return INSTANCE;
    }

    public boolean isDebug() {
        return debug;
    }

    /**
     * @return {@code true} iff web application is not stopped
     */
    public boolean isRunning() {
        return running;
    }

    void stop() {
        this.running = false;
    }

    public int getTemplatesUpdateDelay() {
        return templatesUpdateDelay;
    }

    public List<File> getReloadingClassPaths() {
        return Collections.unmodifiableList(reloadingClassPaths);
    }

    public List<String> getClassReloadingPackages() {
        return Collections.unmodifiableList(classReloadingPackages);
    }

    public List<String> getClassReloadingExceptions() {
        return Collections.unmodifiableList(classReloadingExceptions);
    }

    void setDebug(boolean debug) {
        this.debug = debug;
    }

    void setSkipRegex(Pattern skipRegex) {
        this.skipRegex = skipRegex;
    }

    public Pattern getSkipRegex() {
        return skipRegex;
    }

    void setTemplatesUpdateDelay(int templatesUpdateDelay) {
        this.templatesUpdateDelay = templatesUpdateDelay;
    }

    void setReloadingClassPaths(List<File> reloadingClassPaths) {
        this.reloadingClassPaths = reloadingClassPaths;
    }

    void setClassReloadingPackages(List<String> classReloadingPackages) {
        this.classReloadingPackages = classReloadingPackages;
    }

    void setClassReloadingExceptions(List<String> classReloadingExceptions) {
        this.classReloadingExceptions = classReloadingExceptions;
    }

    void addReloadingClassPath(File dir) {
        if (!dir.isDirectory()) {
            logger.error("Path " + dir.getName() + " expected to be a directory.");
            throw new ConfigurationException("Path " + dir.getName() + " expected to be a directory.");
        }
        if (!reloadingClassPaths.contains(dir)) {
            reloadingClassPaths.add(dir);
        }
    }

    public void addClassReloadingException(String packageOrClassName) {
        classReloadingExceptions.add(packageOrClassName);
    }
}
