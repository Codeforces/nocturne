package bloggy.web.page;

import bloggy.dao.DateDao;
import bloggy.model.User;
import com.google.inject.Inject;
import org.nocturne.main.ApplicationContext;
import org.nocturne.main.Page;

public abstract class ApplicationPage extends Page {
    private static final String USER_SESSION_KEY = "user";
    static final String MESSAGE_SESSION_KEY = "message";

    @Inject
    private DateDao dateDao;

    @Override
    public void initializeAction() {
        super.initializeAction();

        putGlobal("locale", ApplicationContext.getInstance().getLocale().getLanguage().toLowerCase());
        if (getUser() != null) {
            putGlobal("user", getUser());
        }

        putGlobal("now", dateDao.findNow());
    }

    User getUser() {
        return getSession(USER_SESSION_KEY, User.class);
    }

    void authenticate(User user) {
        putSession("user", user);
    }

    void logout() {
        removeSession(USER_SESSION_KEY);
    }

    void setMessage(String message) {
        putSession(MESSAGE_SESSION_KEY, message);
    }
}
