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
import java.util.List;

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
public class DispatchFilter implements Filter {
    private static ReloadingContext reloadingContext
            = ReloadingContext.getInstance();

    private static long lastDebugModeAccess = 0;
    private static long lastDebugModeAccessReloadingClassPathHashCode = 0;
    static ClassLoader lastReloadingClassLoader;
    private static Object debugModeRequestDispatcher;

    private static RequestDispatcher productionModeRequestDispatcher
            = new RequestDispatcher();
    private static FilterConfig filterConfig;

    public void init(FilterConfig config) throws ServletException {
        if (reloadingContext.isDebug()) {
            initDebugMode(config);
        } else {
            productionModeRequestDispatcher.init(config);
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
                debugModeRequestDispatcher = lastReloadingClassLoader.loadClass(RequestDispatcher.class.getName()).newInstance();
                ReflectionUtil.invoke(debugModeRequestDispatcher, "setReloadingClassLoader", lastReloadingClassLoader);
                initDebugMode(filterConfig);
            } catch (Exception e) {
                throw new NocturneException("Can't load request dispatcher.", e);
            }
        }
    }

    private static synchronized void updateReloadingClassLoader() {
        if (lastReloadingClassLoader == null) {
            lastReloadingClassLoader = new ReloadingClassLoader();
            lastDebugModeAccessReloadingClassPathHashCode = hashCode(reloadingContext.getReloadingClassPaths());
            lastDebugModeAccess = System.currentTimeMillis();
        } else {
            if (System.currentTimeMillis() - lastDebugModeAccess > 1000) {
                //long start = System.currentTimeMillis();
                long hashCode = hashCode(reloadingContext.getReloadingClassPaths());
                //System.out.println("DEBUG: hashCode invoked in " + (System.currentTimeMillis() - start) + " ms.");

                if (hashCode != lastDebugModeAccessReloadingClassPathHashCode) {
                    //System.out.println("DEBUG: ReloadingClassLoader created because of hash mismatch.");
                    lastReloadingClassLoader = new ReloadingClassLoader();
                    lastDebugModeAccessReloadingClassPathHashCode = hashCode;
                } else {
                    //System.out.println("DEBUG: Hashes matched");
                }
                lastDebugModeAccess = System.currentTimeMillis();
            }
        }
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
            for (File nested : files) {
                result += hashCode(nested, depth + 1);
            }
        }
        return result;
    }

    private static boolean useInHashCode(File file) {
        String ext = FileUtil.getExt(file);
        return ".class".equalsIgnoreCase(ext)
                || ".properties".equalsIgnoreCase(ext);
    }
//
//    static {
//        ReloadingContextLoader.run();
//    }
}
