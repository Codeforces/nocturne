/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.link;

import org.nocturne.main.Page;

import java.util.Map;

/**
 * It is the result for method Links.match(String). Contains information about
 * matched page and matching itself.
 *
 * @author Mike Mirzayanov
 * @author Andrew Lazarev
 */
public class LinkMatchResult {
    /**
     * Page matched by given link.
     */
    private final Class<? extends Page> pageClass;

    /**
     * Part of link value, matched pattern.
     */
    private final String pattern;

    /**
     * Attributes extracted from given link.
     */
    private final Map<String, String> attributes;

    /**
     * Matched link directive.
     */
    private final Link link;

    /**
     * Constructor LinkMatchResult creates a new LinkMatchResult instance.
     *
     * @param pageClass  Matched page class.
     * @param pattern    Matched pattern (link value).
     * @param attributes attributes extracted from given matching.
     * @param link       Matched @Link instance.
     */
    public LinkMatchResult(Class<? extends Page> pageClass, String pattern, Map<String, String> attributes, Link link) {
        this.pageClass = pageClass;
        this.pattern = pattern;
        this.attributes = attributes;
        this.link = link;
    }

    /**
     * @return Matched page class.
     */
    public Class<? extends Page> getPageClass() {
        return pageClass;
    }

    /**
     * @return Pattern which was matched.
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @return Attributes which was extracted from matching.
     *         For example, it pattern="user/{login}" and Links.match()
     *         argument is "user/mike"
     *         then the returned map will be {@literal {"login"=>"mike"}}.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * @return Returns matched link.
     */
    public Link getLink() {
        return link;
    }
}
