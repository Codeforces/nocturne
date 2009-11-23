/*
 * Copyright 2009 Mike Mirzayanov
 */

package webmail.page.frame;

import org.nocturne.link.Links;
import webmail.page.LoginPage;
import webmail.page.LogoutPage;
import webmail.page.RegisterPage;

import java.util.ArrayList;
import java.util.List;

/** @author Mike Mirzayanov */
public class MenuFrame extends ApplicationFrame {
    private List<Link> links;

    @Override
    public void action() {
        links = new ArrayList<Link>();

        if (getUser() == null) {
            setupLinksForAnonymousUser();
        } else {
            setupLinksForAuthorizedUser();
        }

        put("links", links);
    }

    private void setupLinksForAuthorizedUser() {
        links.add(new Link(Links.getLink(LogoutPage.class), $("Logout")));
    }

    private void setupLinksForAnonymousUser() {
        links.add(new Link(Links.getLink(LoginPage.class), $("Enter")));
        links.add(new Link(Links.getLink(RegisterPage.class), $("Register")));
    }

    public static class Link {
        private final String address;
        private final String text;

        public Link(String address, String text) {
            this.address = address;
            this.text = text;
        }

        public String getAddress() {
            return address;
        }

        public String getText() {
            return text;
        }
    }
}
