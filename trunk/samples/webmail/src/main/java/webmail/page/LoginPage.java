/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import com.google.inject.Inject;
import org.nocturne.annotation.Action;
import org.nocturne.annotation.Parameter;
import org.nocturne.annotation.Validate;
import org.nocturne.link.Link;
import org.nocturne.validation.ValidationException;
import org.nocturne.validation.Validator;
import webmail.dao.UserDao;
import webmail.model.User;

/** @author Mike Mirzayanov */
@Link("")
public class LoginPage extends WebPage {
    @Parameter
    private String login;

    @Parameter(stripMode = Parameter.StripMode.NONE)
    private String password;

    @Inject
    private UserDao userDao;

    private User userForLogin;

    @Validate("login")
    public boolean validateLogin() {
        addValidator("password", new Validator() {
            @Override
            public void run(String value) throws ValidationException {
                userForLogin = userDao.authenticate(login, password);

                if (userForLogin == null) {
                    throw new ValidationException($("Invalid login or password"));
                }
            }
        });
        return runValidation();
    }

    @Action("login")
    public void onLogin() {
        putSession(AUTHORIZED_USER_SESSION_KEY, userForLogin);
        abortWithRedirect(UserPage.class);
    }

    @Override
    public void initializeAction() {
        super.initializeAction();
        if (getUser() != null) {
            abortWithRedirect(UserPage.class);
        }
    }

    @Override
    public void action() {
        // No operations.
    }

    @Override
    public String getTitle() {
        return $("Login");
    }
}
