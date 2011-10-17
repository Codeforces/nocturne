/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import org.apache.log4j.Logger;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.pool.PagePool;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Loads pages from the pool.
 *
 * @author Mike Mirzayanov
 */
public class PageLoader {
    private static final Logger logger = Logger.getLogger(PageLoader.class);

    private RequestRouter requestRouter;
    private final ConcurrentMap<String, PagePool> pagePoolMap
            = new ConcurrentHashMap<String, PagePool>();

    private static final Lock lock = new ReentrantLock();

    private volatile boolean initialized = false;

    void initialize() {
        if (!initialized) {
            lock.lock();
            try {
                if (requestRouter == null) {
                    try {
                        requestRouter = (RequestRouter) getClass().getClassLoader().loadClass(
                                ApplicationContext.getInstance().getRequestRouter()
                        ).newInstance();
                    } catch (Exception e) {
                        throw new ConfigurationException("Can't load application page class name resolver.", e);
                    }
                }
                initialized = true;
            } finally {
                lock.unlock();
                logger.info("Page loader has been initialized.");
            }
        }
    }

    public Page loadPage(String path, Map<String, String> parameterMap) {
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
    public void unloadPage(String path, Map<String, String> parameterMap, Page page) {
        String className = ApplicationContext.getInstance().getRequestPageClassName();
        PagePool pool = getPoolByClassName(className);
        pool.release(page);
    }

    private PagePool getPoolByClassName(String className) {
        PagePool pool = pagePoolMap.get(className);

        if (pool == null) {
            pagePoolMap.putIfAbsent(className, new PagePool(this, className));
        }

        return pagePoolMap.get(className);
    }

    @SuppressWarnings({"unchecked"})
    public Page loadPage(String pageClassName) {
        try {
            Class<Page> pageClass = (Class<Page>)
                    PageLoader.class.getClassLoader().loadClass(pageClassName);
            return ApplicationContext.getInstance().getInjector().getInstance(pageClass);
        } catch (Exception e) {
            throw new ConfigurationException("Can't load page " +
                    pageClassName + '.', e);
        }
    }

    public void close() {
        Collection<PagePool> values = pagePoolMap.values();
        PagePool[] pools = values.toArray(new PagePool[values.size()]);

        for (PagePool pool : pools) {
            pool.close();
        }
   }
}
