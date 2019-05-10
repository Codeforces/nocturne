package bloggy.web.page;

import org.nocturne.annotation.Action;
import org.nocturne.link.Link;
import bloggy.web.annotation.PostOnly;

@Link("logout")
public class LogoutDataPage extends DataPage {
    @PostOnly
    @Action("logout")
    public void action() {
        logout();
        printSuccessJson();
    }
}
