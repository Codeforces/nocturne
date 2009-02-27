package org.nocturne.page;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import org.nocturne.misc.ApplicationContext;
import org.nocturne.pool.PagePool;

import java.util.HashMap;
import java.util.Map;

/** @author Mike Mirzayanov */
public class PageLoader {
    private Injector injector = null;
    private ApplicationContext applicationContext;
    private PageClassNameResolver pageClassNameResolver;
    private Map<String, PagePool> pagePoolMap = new HashMap<String, PagePool>();

    public PageLoader() {
    }

    public PageLoader(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    private Injector getInjector() {
        return injector;
    }

    private void initialize() {
        if (injector == null) {
            String guiceModuleClassName = applicationContext.getGuiceModuleClassName();
            Module module;
            try {
                module = (Module) getClass().getClassLoader().loadClass(
                        guiceModuleClassName
                ).newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Can't load application giuce module.", e);
            }

            try {
                pageClassNameResolver = (PageClassNameResolver) getClass().getClassLoader().loadClass(
                        applicationContext.getPageClassNameResolver()
                ).newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Can't load application page class name resolver.", e);
            }

            injector = Guice.createInjector(module);
        }
    }

    public synchronized Page loadPage(String path, Map<String, String> parameterMap) {
        initialize();

        String className = pageClassNameResolver.getPageClassName(path, parameterMap);
        PagePool pool = getPoolByClassName(className);

        return pool.getInstance();
    }

    public synchronized void unloadPage(String path, Map<String, String> parameterMap, Page page) {
        String className = pageClassNameResolver.getPageClassName(path, parameterMap);
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

    public synchronized Page loadPage(String pageClassName) {
        try {
            Class<Page> pageClass = (Class<Page>)
                    PageLoader.class.getClassLoader().loadClass(pageClassName);
            return getInjector().getInstance(pageClass);
        } catch (Exception e) {
            throw new IllegalStateException("Can't load page " +
                    pageClassName + ".", e);
        }
    }

    public synchronized void close() {
        for (String clazz: pagePoolMap.keySet()) {
            PagePool pool = pagePoolMap.get(clazz);
            pool.close();
        }
    }
}
