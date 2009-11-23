/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import com.google.inject.Inject;
import org.nocturne.annotation.Parameter;
import org.nocturne.link.Link;
import webmail.dao.UserDao;

import java.util.List;

/** @author Mike Mirzayanov */
@Link("suggestUser")
public class SuggestUserPage extends DataPage {
    @Parameter(name = "q")
    private String substring;

    @Inject
    private UserDao userDao;

    @Override
    public void action() {
        if (getUser() != null) {
            List<String> logins = userDao.findLoginsBySubstring(substring);
            for (String login : logins) {
                if (!login.equals(getUser().getLogin())) {
                    getWriter().println(login);
                }
            }
        }
    }
}
