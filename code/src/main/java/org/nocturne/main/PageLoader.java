/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.ModuleInitializationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.pool.PagePool;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Loads pages from the pool.
 *
 * @author Mike Mirzayanov
 */
public class PageLoader {
    private Injector injector = null;
    private RequestRouter requestRouter;
    private final Map<String, PagePool> pagePoolMap = new HashMap<String, PagePool>();

    private static ReentrantLock lock = new ReentrantLock();
    private static boolean loaded = false;

    private void initialize() {
        lock.lock();
        try {
            if (injector == null) {
                setupInjector();
            }
            if (!loaded) {
                runModuleStartups();
                loaded = true;
            }
            if (requestRouter == null) {
                try {
                    requestRouter = (RequestRouter) getClass().getClassLoader().loadClass(
                            ApplicationContext.getInstance().getRequestRouter()
                    ).newInstance();
                } catch (Exception e) {
                    throw new ConfigurationException("Can't load application page class name resolver.", e);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public synchronized Page loadPage(String path, Map<String, String> parameterMap) {
        initialize();

        RequestRouter.Resolution resolution = requestRouter.route(path, parameterMap);
        if (resolution == null && ApplicationContext.getInstance().getDefaultPageClassName() != null) {
            resolution = new RequestRouter.Resolution(ApplicationContext.getInstance().getDefaultPageClassName(), "");
        }

        if (resolution == null) {
            return null;
        } else {
            ApplicationContext.getInstance().setRequestAction(resolution.getAction());
            ApplicationContext.getInstance().setRequestPageClassName(resolution.getPageClassName());

            Map<String, String> overrideParameters = resolution.getOverrideParameters();
            if (overrideParameters != null) {
                for (Map.Entry<String, String> entry : overrideParameters.entrySet()) {
                    ApplicationContext.getInstance().addRequestOverrideParameter(entry.getKey(), entry.getValue());
                }
            }

            PagePool pool = getPoolByClassName(resolution.getPageClassName());
            return pool.getInstance();
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public synchronized void unloadPage(String path, Map<String, String> parameterMap, Page page) {
        String className = ApplicationContext.getInstance().getRequestPageClassName();
        PagePool pool = getPoolByClassName(className);
        pool.release(page);
    }

    private PagePool getPoolByClassName(String className) {
        PagePool pool;

        if (pagePoolMap.containsKey(className)) {
            pool = pagePoolMap.get(className);
        } else {
            pool = new PagePool(this, className);
            pagePoolMap.put(className, pool);
        }
        return pool;
    }

    private void setupInjector() {
        String guiceModuleClassName = ApplicationContext.getInstance().getGuiceModuleClassName();
        GenericIocModule module = new GenericIocModule();

        if (guiceModuleClassName != null) {
            try {
                Module applicationModule = (Module) getClass().getClassLoader().loadClass(
                        guiceModuleClassName
                ).newInstance();
                module.setModule(applicationModule);
            } catch (Exception e) {
                throw new ConfigurationException("Can't load application giuce module.", e);
            }
        }

        injector = Guice.createInjector(module);

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

    private void runModuleStartups() {
        List<org.nocturne.module.Module> modules = ApplicationContext.getInstance().getModules();
        for (org.nocturne.module.Module module : modules) {
            String startupClassName = module.getStartupClassName();
            if (!startupClassName.isEmpty()) {
                Runnable runnable;
                try {
                    runnable = (Runnable) injector.getInstance(getClass().getClassLoader().loadClass(startupClassName));
                } catch (ClassCastException e) {
                    throw new ModuleInitializationException("Startup class " + startupClassName + " must implement Runnable.", e);
                } catch (ClassNotFoundException e) {
                    throw new ModuleInitializationException("Can't load startup class be name " + startupClassName + ".", e);
                }
                if (runnable != null) {
                    runnable.run();
                }
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    public synchronized Page loadPage(String pageClassName) {
        try {
            Class<Page> pageClass = (Class<Page>)
                    PageLoader.class.getClassLoader().loadClass(pageClassName);
            return injector.getInstance(pageClass);
        } catch (Exception e) {
            throw new ConfigurationException("Can't load page " +
                    pageClassName + ".", e);
        }
    }

    public synchronized void close() {
        for (String clazz : pagePoolMap.keySet()) {
            PagePool pool = pagePoolMap.get(clazz);
            pool.close();
        }
    }
}
