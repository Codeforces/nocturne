package bloggy.web.page;

import bloggy.dao.UserDao;
import bloggy.web.annotation.PostOnly;
import com.google.inject.Inject;
import org.nocturne.annotation.Action;
import org.nocturne.annotation.Parameter;
import org.nocturne.annotation.Validate;
import org.nocturne.link.Link;
import org.nocturne.validation.RequiredValidator;
import org.nocturne.validation.ValidationException;
import org.nocturne.validation.Validator;

@Link("enter")
public class EnterPage extends WebPage {
    @Inject
    private UserDao userDao;

    @PostOnly
    @Validate("enter")
    public boolean validateEnter(@Parameter(name = "login", stripMode = Parameter.StripMode.NONE) String login,
                                 @Parameter(name = "password", stripMode = Parameter.StripMode.NONE) String password) {
        addValidator("login", new RequiredValidator());
        addValidator("password", new RequiredValidator());
        addValidator("password", new Validator() {
            @Override
            public void run(String ignored) throws ValidationException {
                if (userDao.findByLoginAndPassword(login, password) == null) {
                    throw new ValidationException($("Invalid login or password"));
                }
            }
        });
        return runValidation();
    }

    @PostOnly
    @Action("enter")
    public void onEnter(@Parameter(name = "login", stripMode = Parameter.StripMode.NONE) String login,
                        @Parameter(name = "password", stripMode = Parameter.StripMode.NONE) String password) {
        authenticate(userDao.findByLoginAndPassword(login, password));
        abortWithRedirect(IndexPage.class);
    }

    @Override
    public void action() {
        // No operations.
    }
}
