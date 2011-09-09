package org.nocturne.link.pages;

import org.nocturne.link.Link;
import org.nocturne.main.Page;

/**
 * @author Mike Mirzayanov
 */
@Link(value = "profile/{userName:Mike,Max};profiles/all", name = "ProfilePage")
public class ProfilePage extends Page {
    @Override
    public void action() {
    }
}
