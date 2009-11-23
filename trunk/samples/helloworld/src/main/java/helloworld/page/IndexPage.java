/*
 * Copyright 2009 Mike Mirzayanov
 */

package helloworld.page;

import org.nocturne.link.Link;
import org.nocturne.main.Page;

/** @author Mike Mirzayanov */
@Link("")
public class IndexPage extends Page {
    @Override
    public void action() {
        put("message", "Hello, world");
    }
}
