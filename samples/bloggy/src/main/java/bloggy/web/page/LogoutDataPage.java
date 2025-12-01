package bloggy.web.page;

import bloggy.web.annotation.PostOnly;
import org.nocturne.annotation.Action;
import org.nocturne.link.Link;

@Link("logout")
public class LogoutDataPage extends DataPage {
    @PostOnly
    @Action("logout")
    public void action() {
        logout();
        printSuccessJson();
    }
}
