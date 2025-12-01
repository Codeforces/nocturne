package bloggy.web.frame;

import bloggy.dao.UserDao;
import bloggy.model.Post;
import bloggy.model.User;
import com.google.inject.Inject;

public class PostViewFrame extends ApplicationFrame {
    @Inject
    private UserDao userDao;

    @Inject
    private UserpicFrame userpicFrame;

    private boolean shortMode;
    private Post post;

    public void setShortMode(boolean shortMode) {
        this.shortMode = shortMode;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    @Override
    public void action() {
        put("shortMode", shortMode);
        put("post", post);
        User postUser = userDao.find(post.getUserId());
        put("postUser", postUser);
        userpicFrame.setUserpicUser(postUser);
        parse("userpicFrame", userpicFrame);
    }
}
