package bloggy.web.frame;

import bloggy.model.User;

public class UserpicFrame extends ApplicationFrame {
    private static final int USERPIC_COUNT = 4;

    private User userpicUser;

    /** @noinspection WeakerAccess*/
    public void setUserpicUser(User userpicUser) {
        this.userpicUser = userpicUser;
    }

    @Override
    public void action() {
        put("userpicIndex", userpicUser.getId() % USERPIC_COUNT);
        put("userpicUser", userpicUser);

    }
}
