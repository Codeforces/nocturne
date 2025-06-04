package ${packageName};

import ${packageName}.page.IndexPage;
import org.nocturne.main.LinkedRequestRouter;
import org.nocturne.link.Links;

/**
 * Sample application uses subclass of LinkedRequestRouter as 
 * a RequestRouter.
 * <p/>
 * Just add into the links your pages, like: <@code
 *     // Your page should have @Link annotation.
 *     Links.add(IndexPage.class);
 * } 
 */
public class ApplicationRequestRouter extends LinkedRequestRouter {
    static {
        Links.add(IndexPage.class);
    }
}
