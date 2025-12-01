package bloggy.web.page;

import bloggy.model.User;
import bloggy.web.frame.PostsViewFrame;
import com.google.inject.Inject;
import org.nocturne.link.Link;

@Link("my")
public class MyPostsPage extends WebPage {
    @Inject
    private PostsViewFrame postsViewFrame;

    @Override
    public void action() {
        User user = getUser();
        if (user == null) {
            abortWithRedirect(EnterPage.class);
        } else {
            postsViewFrame.setPostsUser(user);
            parse("postsViewFrame", postsViewFrame);
        }
    }
}
