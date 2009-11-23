/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page.frame;

import org.nocturne.main.Frame;
import webmail.model.User;

/** @author Mike Mirzayanov */
public abstract class ApplicationFrame extends Frame {
    public User getUser() {
        return getSession("authorizedUser", User.class);
    }
}
