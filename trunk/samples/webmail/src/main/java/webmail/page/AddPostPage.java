/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import com.google.gson.Gson;
import com.google.inject.Inject;
import org.nocturne.annotation.Parameter;
import org.nocturne.link.Link;
import org.nocturne.validation.LengthValidator;
import org.nocturne.validation.RequiredValidator;
import org.nocturne.validation.ValidationException;
import org.nocturne.validation.Validator;
import webmail.dao.PostDao;
import webmail.dao.UserDao;

import java.net.HttpURLConnection;

/** @author Mike Mirzayanov */
@Link("addPost")
public class AddPostPage extends DataPage {
    @Parameter
    private String to;

    @Parameter(stripMode = Parameter.StripMode.NONE)
    private String text;

    @Inject
    private Gson gson;

    @Inject
    private PostDao postDao;

    @Inject
    private UserDao userDao;

    @Override
    public void initializeAction() {
        super.initializeAction();
        
        if (getUser() == null) {
            abortWithError(HttpURLConnection.HTTP_FORBIDDEN);
        }
    }

    public boolean validate() {
        addValidator("to", new RequiredValidator());
        addValidator("to", new Validator() {
            @Override
            public void run(String value) throws ValidationException {
                if (userDao.isLoginFree(value)) {
                    throw new ValidationException($("No such user, use autocompletion"));
                }
            }
        });

        addValidator("text", new RequiredValidator());
        addValidator("text", new LengthValidator(1, 4096));

        return runValidationAndPrintErrors();
    }

    @Override
    public void action() {
        postDao.addPost(getUser(), userDao.findByLogin(to), text);
    }
}
