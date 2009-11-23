/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import org.nocturne.link.Link;

/** @author Mike Mirzayanov */
@Link("logout")
public class LogoutPage extends WebPage {
    @Override
    public String getTitle() {
        return $("Logout");
    }

    @Override
    public void initializeAction() {
        super.initializeAction();
        skipTemplate();
    }

    @Override
    public void action() {
        removeSession(AUTHORIZED_USER_SESSION_KEY);
        abortWithRedirect(LoginPage.class);
    }
}
