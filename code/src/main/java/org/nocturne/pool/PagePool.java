/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.pool;

import org.nocturne.main.Page;
import org.nocturne.main.PageLoader;
import org.apache.log4j.Logger;

/**
 * Stores all the instances of the specific page class.
 *
 * @author Mike Mirzayanov
 */
public class PagePool extends Pool<Page> {
    private static final Logger logger = Logger.getLogger(PagePool.class);

    /** Generates page instances. */
    private final PageLoader pageLoader;

    /**  */
    private final String pageClassName;

    /**
     * Constructor PagePool creates a new PagePool instance.
     *
     * @param pageLoader of type PageLoader
     * @param pageClassName of type String
     */
    public PagePool(PageLoader pageLoader, String pageClassName) {
        this.pageLoader = pageLoader;
        this.pageClassName = pageClassName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Page newInstance() {
        return pageLoader.loadPage(pageClassName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int getAcquireIncrement() {
        int createdCount = getCreatedCount();

        if (createdCount < 1000) {
            int result = Math.max(25, createdCount + 1);
            logger.warn("PagePool will create " + pageClassName + ": " + result + " [large step]");
            return result;
        } else {
            logger.warn("PagePool will create " + pageClassName + ": " + 25 + " [small step]");
            return 25;
        }
    }
}
