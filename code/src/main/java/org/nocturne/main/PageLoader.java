/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import com.google.common.base.Throwables;
import org.apache.log4j.Logger;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.pool.PagePool;

import java.util.Collection;
import java.util.List;
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
@SuppressWarnings("WeakerAccess")
public class PageLoader {
    private static final Logger logger = Logger.getLogger(PageLoader.class);

    private RequestRouter requestRouter;
    private final ConcurrentMap<String, PagePool> pagePoolMap = new ConcurrentHashMap<>();

    private static final Lock lock = new ReentrantLock();

    private volatile boolean initialized;

    void initialize() {
        if (!initialized) {
            lock.lock();
            try {
                if (requestRouter == null) {
                    try {
                        requestRouter = (RequestRouter) getClass().getClassLoader().loadClass(
                                ApplicationContext.getInstance().getRequestRouter()
                        ).getConstructor().newInstance();
                    } catch (NoSuchMethodException e) {
                        throw new ConfigurationException(
                                "Application page class name resolver does not have default constructor.", e
                        );
                    } catch (Exception e) {
                        throw new ConfigurationException("Can't load application page class name resolver.", e);
                    }
                }
                initialized = true;
            } finally {
                lock.unlock();
                logger.debug("Page loader has been initialized.");
            }
        }
    }

    public Page loadPage(String path, Map<String, List<String>> parameterMap) {
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

            Map<String, List<String>> overrideParameters = resolution.getOverrideParameters();
            if (overrideParameters != null) {
                for (Map.Entry<String, List<String>> entry : overrideParameters.entrySet()) {
                    ApplicationContext.getInstance().addRequestOverrideParameter(entry.getKey(), entry.getValue());
                }
            }

            PagePool pool = getPoolByClassName(resolution.getPageClassName());
            return pool.getInstance();
        }
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public void unloadPage(String path, Map<String, List<String>> parameterMap, Page page) {
        String className = ApplicationContext.getInstance().getRequestPageClassName();
        PagePool pool = getPoolByClassName(className);
        pool.release(page);
    }

    private PagePool getPoolByClassName(String className) {
        PagePool pool = pagePoolMap.get(className);

        if (pool == null) {
            pagePoolMap.putIfAbsent(className, new PagePool(this, className));
            pool = pagePoolMap.get(className);
        }

        return pool;
    }

    @SuppressWarnings({"unchecked", "MethodMayBeStatic"})
    public Page loadPage(String pageClassName) {
        try {
            Class<Page> pageClass = (Class<Page>) PageLoader.class.getClassLoader().loadClass(pageClassName);
            return ApplicationContext.getInstance().getInjector().getInstance(pageClass);
        } catch (Exception e) {
            logger.error("Can't load page " + pageClassName + '.', e);
            throw new ConfigurationException("Can't load page " + pageClassName + '.', e);
        }
    }

    public void close() {
        Collection<PagePool> values = pagePoolMap.values();
        PagePool[] pools = values.toArray(new PagePool[0]);

        for (PagePool pool : pools) {
            pool.close();
        }
    }
}
