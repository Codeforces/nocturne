/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import com.google.inject.Inject;
import webmail.page.frame.MenuFrame;

/** @author Mike Mirzayanov */
public abstract class WebPage extends ApplicationPage {
    @Inject
    private MenuFrame menuFrame;

    public abstract String getTitle();

    @Override
    public void initializeAction() {
        super.initializeAction();
        putGlobal("user", getUser());
        put("pageTitle", getTitle());
    }

    @Override
    public void finalizeAction() {
        parse("menuFrame", menuFrame);
        super.finalizeAction();
    }
}
