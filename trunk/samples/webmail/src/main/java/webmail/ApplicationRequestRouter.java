package webmail;

import org.nocturne.link.LinkedRequestRouter;
import org.nocturne.link.Links;
import webmail.page.*;

/**
 * @author Mike Mirzayanov
 */
public class ApplicationRequestRouter extends LinkedRequestRouter {
    static {
        Links.add(LoginPage.class);
        Links.add(LogoutPage.class);
        Links.add(RegisterPage.class);
        Links.add(UserPage.class);
        Links.add(SuggestUserPage.class);
        Links.add(AddPostPage.class);
    }
}
