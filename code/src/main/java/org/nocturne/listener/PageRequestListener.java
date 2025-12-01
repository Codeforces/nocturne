/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.listener;

import org.nocturne.main.Page;

/**
 * You can implement this interface to handle
 * requests to application pages.
 *
 * @author Mike Mirzayanov
 */
public interface PageRequestListener {
    /**
     * Will be called before processing specified page. Just
     * after request routing.
     *
     * @param page Page to be processed.
     */
    void beforeProcessPage(Page page);

    /**
     * Will be called after request processed the page.
     * It doesn't matter if page fails with exception - this
     * method will be executed.
     *
     * @param page Processed page.
     * @param t    {@code null} if no throwable has been thrown.
     */
    void afterProcessPage(Page page, Throwable t);
}
