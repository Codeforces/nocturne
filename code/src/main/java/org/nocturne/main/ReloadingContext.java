/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import org.nocturne.exception.ConfigurationException;

import java.io.File;
import java.util.List;

/**
 * @author Mike Mirzayanov
 */
class ReloadingContext {
    private static final ReloadingContext INSTANCE =
            new ReloadingContext();

    private boolean debug;
    private List<File> reloadingClassPaths;
    private List<String> classReloadingPackages;
    private List<String> classReloadingExceptions;

    private ReloadingContext() {
    }

    public static ReloadingContext getInstance() {
        return INSTANCE;
    }

    public boolean isDebug() {
        return debug;
    }

    public List<File> getReloadingClassPaths() {
        return reloadingClassPaths;
    }

    public List<String> getClassReloadingPackages() {
        return classReloadingPackages;
    }

    public List<String> getClassReloadingExceptions() {
        return classReloadingExceptions;
    }

    void setDebug(boolean debug) {
        this.debug = debug;
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
            throw new ConfigurationException("Path " + dir.getName() + " exected to be a directory.");
        }
        if (reloadingClassPaths.indexOf(dir) < 0) {
            reloadingClassPaths.add(dir);
        }
    }
}
