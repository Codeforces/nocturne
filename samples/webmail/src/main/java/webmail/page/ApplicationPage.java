/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import org.nocturne.main.Page;
import webmail.model.User;

/** @author Mike Mirzayanov */
public abstract class ApplicationPage extends Page {
    protected static final String AUTHORIZED_USER_SESSION_KEY = "authorizedUser";

    public User getUser() {
        return getSession(AUTHORIZED_USER_SESSION_KEY, User.class);
    }
}
