package org.nocturne.pool;

import org.nocturne.page.Page;
import org.nocturne.page.PageLoader;

/** @author Mike Mirzayanov */
public class PagePool extends Pool<Page> {
    private final PageLoader pageLoader;
    private final String pageClassName;

    public PagePool(PageLoader pageLoader, String pageClassName) {
        this.pageLoader = pageLoader;
        this.pageClassName = pageClassName;
    }

    protected Page newInstance() {
        return pageLoader.loadPage(pageClassName);
    }
}
