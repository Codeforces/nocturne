package org.nocturne.link.pages;

import org.nocturne.link.Link;
import org.nocturne.link.LinkSet;
import org.nocturne.main.Page;

/**
 * @author Mike Mirzayanov
 */
@LinkSet({
        @Link(value = ";index;page/{pageIndex};index/page/{pageIndex}"),
        @Link(value = "vipIndex;vipPage/{pageIndex}", name = "vipLink")
})
public class IndexPage extends Page {
    @Override
    public void action() {
    }
}
