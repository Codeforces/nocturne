/* Copyright by Mike Mirzayanov. */

package org.nocturne.page;

/**
 * You can implement this interface to handle
 * requests to application pages.
 *
 * @author Mike Mirzayanov
 */
public interface PageRequestListener {
    void onPageRequest(Page page);
}
