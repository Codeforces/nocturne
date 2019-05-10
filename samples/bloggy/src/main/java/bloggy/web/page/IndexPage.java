package bloggy.web.page;

import com.google.inject.Inject;
import org.nocturne.link.Link;
import bloggy.web.frame.PostsViewFrame;

@Link(";index")
public class IndexPage extends WebPage {
    @Inject
    private PostsViewFrame postsViewFrame;

    @Override
    public void action() {
        parse("postsViewFrame", postsViewFrame);
    }
}
