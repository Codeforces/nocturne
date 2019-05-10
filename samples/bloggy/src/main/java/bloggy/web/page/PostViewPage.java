package bloggy.web.page;

import com.google.inject.Inject;
import org.nocturne.annotation.Parameter;
import org.nocturne.link.Link;
import bloggy.dao.PostDao;
import bloggy.model.Post;
import bloggy.web.frame.PostViewFrame;

@Link("post/{postId}")
public class PostViewPage extends WebPage {
    @Parameter
    private long postId;

    private Post post;

    @Inject
    private PostDao postDao;

    @Inject
    private PostViewFrame postViewFrame;


    @Override
    public void initializeAction() {
        super.initializeAction();

        post = postDao.find(postId);
        if (post == null) {
            setMessage("No such post");
            abortWithRedirect(IndexPage.class);
        }
    }

    @Override
    public void action() {
        postViewFrame.setPost(post);
        postViewFrame.setShortMode(false);
        parse("postViewFrame", postViewFrame);
    }
}
