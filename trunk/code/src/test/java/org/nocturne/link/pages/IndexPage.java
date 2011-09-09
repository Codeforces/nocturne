package org.nocturne.link.pages;

import org.nocturne.link.Link;
import org.nocturne.main.Page;

/**
 * @author Mike Mirzayanov
 */
@Link(value = ";index;page/{pageIndex};index/page/{pageIndex}")
public class IndexPage extends Page {
    @Override
    public void action() {
    }
}
