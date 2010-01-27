/*
 * Copyright 2009 Mike Mirzayanov
 */

package helloworld;

import helloworld.page.IndexPage;
import org.nocturne.main.LinkedRequestRouter;
import org.nocturne.link.Links;

/**
 * @author Mike Mirzayanov
 */
public class ApplicationRequestRouter extends LinkedRequestRouter {
    static {
        Links.add(IndexPage.class);
    }
}
