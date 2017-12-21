/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.pool;

import org.apache.log4j.Logger;
import org.nocturne.main.Page;
import org.nocturne.main.PageLoader;

/**
 * Stores all the instances of the specific page class.
 *
 * @author Mike Mirzayanov
 */
public class PagePool extends Pool<Page> {
    private static final Logger logger = Logger.getLogger(PagePool.class);

    /**
     * Generates page instances.
     */
    private final PageLoader pageLoader;

    /**  */
    private final String pageClassName;

    /**
     * Constructor PagePool creates a new PagePool instance.
     *
     * @param pageLoader    of type PageLoader
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
        return 3;
    }
}
