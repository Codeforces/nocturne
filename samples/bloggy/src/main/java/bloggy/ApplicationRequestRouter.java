package bloggy;

import org.nocturne.link.Links;
import org.nocturne.main.LinkedRequestRouter;
import bloggy.web.page.*;

/**
 * @author Mike Mirzayanov
 */
public class ApplicationRequestRouter extends LinkedRequestRouter {
    static {
        Links.add(IndexPage.class);
        Links.add(MyPostsPage.class);
        Links.add(EnterPage.class);
        Links.add(LogoutDataPage.class);
        Links.add(PostViewPage.class);
    }
}
