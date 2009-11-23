/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import com.google.inject.Inject;
import org.nocturne.annotation.Action;
import org.nocturne.annotation.Parameter;
import org.nocturne.annotation.Validate;
import org.nocturne.link.Link;
import org.nocturne.validation.LengthValidator;
import org.nocturne.validation.ValidationException;
import org.nocturne.validation.Validator;
import org.nocturne.validation.WordValidator;
import webmail.model.User;
import webmail.dao.UserDao;

/** @author Mike Mirzayanov */
@Link("register")
public class RegisterPage extends WebPage {
    @Parameter(stripMode = Parameter.StripMode.NONE)
    private String name;

    @Parameter(stripMode = Parameter.StripMode.NONE)
    private String login;

    @Parameter(stripMode = Parameter.StripMode.NONE)
    private String password;

    @Parameter(stripMode = Parameter.StripMode.NONE)
    private String passwordConfirmation;

    @Inject
    private UserDao userDao;

    @Validate("register")
    public boolean validateRegister() {
        addValidator("name", new LengthValidator(2, 64));
        addValidator("name", new Validator() {
            @Override
            public void run(String value) throws ValidationException {
                if (!value.matches("[-\\.\\w\\s]+")) {
                    throw new ValidationException($("Field can't contain special characters"));
                }
            }
        });

        addValidator("login", new LengthValidator(2, 64));
        addValidator("login", new WordValidator());
        addValidator("login", new Validator() {
            @Override
            public void run(String value) throws ValidationException {
                if (!userDao.isLoginFree(value)) {
                    throw new ValidationException($("Login is already in use, select another"));
                }
            }
        });

        addValidator("password", new LengthValidator(4, 128));
        addValidator("passwordConfirmation", new Validator() {
            @Override
            public void run(String value) throws ValidationException {
                if (!password.equals(passwordConfirmation)) {
                    throw new ValidationException($("Confirmation and password should be equal"));
                }
            }
        });

        return runValidation();
    }

    @Action("register")
    public void onRegister() {
        User user = new User();
        user.setLogin(login);
        user.setName(name);

        if (!userDao.register(user, password)) {
            put("error", $("Can't register, try again"));
        } else {
            putSession(AUTHORIZED_USER_SESSION_KEY, user);
            abortWithRedirect(UserPage.class);
        }
    }

    @Override
    public String getTitle() {
        return $("Registration");
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
}
