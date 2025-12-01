package org.nocturne.link.pages;

import org.nocturne.link.Link;
import org.nocturne.link.LinkSet;
import org.nocturne.main.Page;

/**
 * @author Mike Mirzayanov
 */
@LinkSet({
        @Link(value = "sections/{sectionId:1,2,3,01,02,03}", name = "SectionsPageBySectionId"),
        @Link(value = "sections/{sectionName}", name = "SectionsPageBySectionName")
})
public class SectionsPage extends Page {
    @Override
    public void action() {
    }
}
