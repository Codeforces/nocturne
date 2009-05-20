package org.nocturne.misc;

import com.google.inject.Injector;

import javax.servlet.ServletContext;

/** @author Mike Mirzayanov */
public class ApplicationContext {
    private String templatesPath;
    private boolean debugMode;
    private String guiceModuleClassName;
    private String pageClassNameResolver;
    private String skipRegex;
    private String reloadingClassLoaderPattern;
    private String reloadingClassLoaderClassesPath;
    private ServletContext servletContext;
    private Injector injector;

    public String getReloadingClassLoaderClassesPath() {
        return reloadingClassLoaderClassesPath;
    }

    public void setReloadingClassLoaderClassesPath(String reloadingClassLoaderClassesPath) {
        this.reloadingClassLoaderClassesPath = reloadingClassLoaderClassesPath;
    }

    public String getTemplatesPath() {
        return templatesPath;
    }

    public void setTemplatesPath(String templatesPath) {
        this.templatesPath = templatesPath;
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setGuiceModuleClassName(String guiceModuleClassName) {
        this.guiceModuleClassName = guiceModuleClassName;
    }

    public String getGuiceModuleClassName() {
        return guiceModuleClassName;
    }

    public String getPageClassNameResolver() {
        return pageClassNameResolver;
    }

    public void setPageClassNameResolver(String pageClassNameResolver) {
        this.pageClassNameResolver = pageClassNameResolver;
    }

    public String getSkipRegex() {
        return skipRegex;
    }

    public void setSkipRegex(String skipRegex) {
        this.skipRegex = skipRegex;
    }

    public void setReloadingClassLoaderPattern(String reloadingClassLoaderPattern) {
        this.reloadingClassLoaderPattern = reloadingClassLoaderPattern;
    }

    public String getReloadingClassLoaderPattern() {
        return reloadingClassLoaderPattern;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Injector getInjector() {
        return injector;
    }

    public void setInjector(Injector injector) {
        this.injector = injector;
    }
}
