/* Copyright by Mike Mirzayanov. */

package org.nocturne.page;

/**
 * You can implement this interface to handle
 * requests to application pages.
 *
 * @author Mike Mirzayanov
 */
public interface PageRequestListener {
    /**
     * Will be called before processing specified page.
     * @param page Page to be processed.
     */
    void beforeProcessPage(Page page);

    /**
     * Will be called after request processed the page.
     * @param page Processed page.
     * @param t {@code null} if no throwable has been thrown. 
     */
    void afterProcessPage(Page page, Throwable t);
}
