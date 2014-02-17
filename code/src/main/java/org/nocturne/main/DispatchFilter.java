/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import org.nocturne.exception.NocturneException;
import org.nocturne.exception.ReflectionException;
import org.nocturne.util.FileUtil;
import org.nocturne.util.ReflectionUtil;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Main nocturne filter to dispatch requests.
 * <p/>
 * In will create new ReloadingClassLoader on each request
 * (if changes found and more than 500 ms passed since last request) in the debug mode.
 * This class loader will load updated classes of your application.
 * <p/>
 * In the production mode it uses usual webapp class loader.
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings({"AccessOfSystemProperties", "UseOfPropertiesAsHashtable", "unchecked"})
public class DispatchFilter implements Filter {
    private static final ReloadingContext reloadingContext = ReloadingContext.getInstance();

    static ClassLoader lastReloadingClassLoader;
    private static Object debugModeRequestDispatcher;
    private static long lastDebugModeAccess;
    private static long lastDebugModeAccessReloadingClassPathHashCode;

    private static final RequestDispatcher productionModeRequestDispatcher = new RequestDispatcher();
    private static FilterConfig filterConfig;

    @Override
    public void init(FilterConfig config) throws ServletException {
        productionModeRequestDispatcher.init(config);
        if (reloadingContext.isDebug()) {
            initDebugMode(config);
        }

        filterConfig = config;
    }

    private static void initDebugMode(FilterConfig config) {
        if (debugModeRequestDispatcher != null) {
            try {
                ReflectionUtil.invoke(debugModeRequestDispatcher, "init", config);
            } catch (ReflectionException e) {
                throw new NocturneException("Can't run debug mode request dispatcher init().", e);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        servletRequest.setCharacterEncoding("UTF-8");
        servletResponse.setCharacterEncoding("UTF-8");

        if (servletRequest instanceof HttpServletRequest && servletResponse instanceof HttpServletResponse) {
            HttpServletRequest request = (HttpServletRequest) servletRequest;
            HttpServletResponse response = (HttpServletResponse) servletResponse;

            if (reloadingContext.getSkipRegex() != null && reloadingContext.getSkipRegex().matcher(request.getServletPath()).matches()) {
                filterChain.doFilter(request, response);
            } else {
                if (reloadingContext.isDebug()) {
                    updateRequestDispatcher();

                    try {
                        ReflectionUtil.invoke(debugModeRequestDispatcher, "doFilter", request, response, filterChain);
                    } catch (ReflectionException e) {
                        throw new NocturneException("Can't run debug mode request dispatcher doFilter().", e);
                    }
                } else {
                    productionModeRequestDispatcher.doFilter(request, response, filterChain);
                }
            }
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    @Override
    public void destroy() {
        if (reloadingContext.isDebug()) {
            destroyDebugMode();
        } else {
            productionModeRequestDispatcher.destroy();
        }
    }

    private static void destroyDebugMode() {
        if (debugModeRequestDispatcher != null) {
            try {
                ReflectionUtil.invoke(debugModeRequestDispatcher, "destroy");
            } catch (ReflectionException e) {
                throw new NocturneException("Can't run debug mode request dispatcher destroy().", e);
            }
        }
    }

    static synchronized void updateRequestDispatcher() {
        ClassLoader previousClassLoader = lastReloadingClassLoader;
        updateReloadingClassLoader();
        if (previousClassLoader != lastReloadingClassLoader || debugModeRequestDispatcher == null) {
            try {
                destroyDebugMode();
                debugModeRequestDispatcher = lastReloadingClassLoader.loadClass(
                        RequestDispatcher.class.getName()
                ).getConstructor().newInstance();
                ReflectionUtil.invoke(debugModeRequestDispatcher, "setReloadingClassLoader", lastReloadingClassLoader);
                initDebugMode(filterConfig);
            } catch (Exception e) {
                throw new NocturneException("Can't load request dispatcher.", e);
            }
        }
    }

    private static synchronized void updateReloadingClassLoader() {
        if ("true".equals(System.getProperty("dreamcatcher.loaded"))) {
            updateDreamcatcherReloadingClassLoader();
        } else {
            updateNoDreamcatcherReloadingClassLoader();
        }
    }

    private static void updateNoDreamcatcherReloadingClassLoader() {
        if (lastReloadingClassLoader == null) {
            lastReloadingClassLoader = new ReloadingClassLoader();
            lastDebugModeAccessReloadingClassPathHashCode = hashCode(reloadingContext.getReloadingClassPaths());
            lastDebugModeAccess = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - lastDebugModeAccess > 1000) {
                long hashCode = hashCode(reloadingContext.getReloadingClassPaths());

                if (hashCode != lastDebugModeAccessReloadingClassPathHashCode) {
                    lastReloadingClassLoader = new ReloadingClassLoader();
                    lastDebugModeAccessReloadingClassPathHashCode = hashCode;
                }

                lastDebugModeAccess = System.currentTimeMillis();
            }
        }
    }

    private static void updateDreamcatcherReloadingClassLoader() {
        if (lastReloadingClassLoader == null) {
            lastReloadingClassLoader = new ReloadingClassLoader();
        } else {
            if ("true".equals(System.getProperty("dreamcatcher.can-not-redefine-class"))) {
                if (!System.getProperties().containsKey("nocturne.unused-reloading-class-loaders")) {
                    System.getProperties().put("nocturne.unused-reloading-class-loaders", new HashSet<ClassLoader>());
                }

                //noinspection unchecked
                Set<ClassLoader> unusedReloadingClassLoaders
                        = (Set<ClassLoader>) System.getProperties().get("nocturne.unused-reloading-class-loaders");
                if (lastReloadingClassLoader instanceof ReloadingClassLoader) {
                    ReloadingClassLoader reloadingClassLoader = (ReloadingClassLoader) lastReloadingClassLoader;
                    unusedReloadingClassLoaders.add(reloadingClassLoader.getDelegationClassLoader());
                }

                ReloadingClassLoader reloadingClassLoader = new ReloadingClassLoader();
                lastReloadingClassLoader = reloadingClassLoader;

                System.out.println("NOCTURNE: ReloadingClassLoader created because of dreamcatcher.can-not-redefine-class=true"
                        + " [reloadingClassLoader=" + reloadingClassLoader
                        + ", delegationClassLoader=" + reloadingClassLoader.getDelegationClassLoader()
                        + "]");
            }
        }

        System.setProperty("dreamcatcher.can-not-redefine-class", "false");
    }

    private static long hashCode(List<File> paths) {
        long result = 0;
        long mul = 1;
        for (File dir : paths) {
            result += dir.hashCode() * mul;
            mul *= 31;
            result += hashCode(dir, 0) * mul;
            mul *= 31;
        }
        return result;
    }

    private static long hashCode(File file, long depth) {
        long result = 0;
        if (file.isFile()) {
            if (useInHashCode(file)) {
                result += file.getName().hashCode() * file.lastModified() * (depth + 1);
            }
        } else {
            File[] files = file.listFiles();
            if (files != null) {
                for (File nested : files) {
                    result += hashCode(nested, depth + 1);
                }
            }
        }
        return result;
    }

    private static boolean useInHashCode(File file) {
        String ext = FileUtil.getExt(file);
        return ".class".equalsIgnoreCase(ext) || ".properties".equalsIgnoreCase(ext);
    }
}
