package bloggy.web.page;

import bloggy.web.frame.PostsViewFrame;
import com.google.inject.Inject;
import org.nocturne.link.Link;

@Link(";index")
public class IndexPage extends WebPage {
    @Inject
    private PostsViewFrame postsViewFrame;

    @Override
    public void action() {
        parse("postsViewFrame", postsViewFrame);
    }
}
