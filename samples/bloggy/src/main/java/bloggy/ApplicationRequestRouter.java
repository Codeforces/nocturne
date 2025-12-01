package bloggy;

import bloggy.web.page.*;
import org.nocturne.link.Links;
import org.nocturne.main.LinkedRequestRouter;

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
