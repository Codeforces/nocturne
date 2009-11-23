/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page;

import com.google.inject.Inject;
import org.nocturne.annotation.Parameter;
import org.nocturne.exception.IncorrectLogicException;
import org.nocturne.link.Link;
import webmail.dao.PostDao;
import webmail.model.Post;

import java.util.List;

/** @author Mike Mirzayanov */
@Link("personal;personal/{mode}")
public class UserPage extends WebPage {
    @Parameter
    private Mode mode;

    @Inject
    private PostDao postDao;

    private List<Post> posts;

    @Override
    public String getTitle() {
        return getUser().getLogin();
    }

    @Override
    public void initializeAction() {
        if (getUser() == null) {
            abortWithRedirect(LoginPage.class);
        }
        
        super.initializeAction();

        if (mode == null) {
            mode = Mode.ALL;
        }

        addJs("js/jquery.smartmodal.js");
        addJs("js/jquery.autocomplete.pack.js");
        addCss("css/modal.css");
        addCss("css/jquery.autocomplete.css");

        setupPosts();
    }

    private void setupPosts() {
        switch (mode) {
            case ALL:
                posts = postDao.findRalatedTo(getUser());
                break;
            case INCOME:
                posts = postDao.findTargetedFor(getUser());
                break;
            case OUTCOME:
                posts = postDao.findAuthoredBy(getUser());
                break;
            default:
                throw new IncorrectLogicException("Unexpected mode " + mode + ".");
        }
    }

    @Override
    public void action() {
        put("mode", mode);
        put("posts", posts);
    }

    public static enum Mode {
        ALL,
        INCOME,
        OUTCOME;
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }
}
