/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.link;

import org.nocturne.annotation.Name;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.main.ApplicationContext;
import org.nocturne.main.Page;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Handles link pattern methods.
 * Each page should have @Link annotation to specify its link.
 * It can use parameters (templates), like "profile/{handle}".
 * <p/>
 * If you want to redirect to SomePage, use abortWithRedirect(Links.getLink(SomePage.class)) or
 * abortWithRedirect(SomePage.class).
 *
 * @author Mike Mirzayanov
 */
public class Links {
    /**
     * Stores maps for each page class. Each map contains single patterns as keys
     * and Link instances as values.
     */
    private static final Map<Class<? extends Page>, Map<String, Link>> linksByPage =
            new ConcurrentHashMap<Class<? extends Page>, Map<String, Link>>();

    /** Stores page classes by their names. */
    private static final Map<String, Class<? extends Page>> classesByName =
            new ConcurrentHashMap<String, Class<? extends Page>>();

    /** Stores link sections by links. */
    private static final ConcurrentMap<String, List<LinkSection>> sectionsByLinkText =
            new ConcurrentHashMap<String, List<LinkSection>>();

    private static List<Link> getLinksViaReflection(Class<? extends Page> clazz) {
        List<Link> result = new ArrayList<Link>();
        Link link = clazz.getAnnotation(Link.class);
        if (link != null) {
            result.add(link);
        }
        LinkSet linkSet = clazz.getAnnotation(LinkSet.class);
        if (linkSet != null) {
            result.addAll(Arrays.asList(linkSet.value()));
        }
        return result;
    }

    private static String getNameViaReflection(Class<? extends Page> clazz) {
        Name name = clazz.getAnnotation(Name.class);

        if (name == null) {
            return clazz.getSimpleName();
        } else {
            return name.value();
        }
    }

    /**
     * @param clazz Page class to be added into Links.
     *              After it you can get it's link via getLink, or using @link directive
     *              from template. Link may contain template sections, like "profile/{handle}".
     */
    public static synchronized void add(Class<? extends Page> clazz) {
        List<Link> linkSet = getLinksViaReflection(clazz);
        if (linkSet.isEmpty()) {
            throw new ConfigurationException("Can't find link for page " + clazz.getName() + '.');
        }

        String name = getNameViaReflection(clazz);
        if (classesByName.containsKey(name) && !clazz.equals(classesByName.get(name))) {
            throw new ConfigurationException("Can't add page which is not unique by it's name: "
                    + clazz.getName() + '.');
        }
        classesByName.put(name, clazz);

        Map<String, Link> links = linksByPage.get(clazz);
        if (links == null) {
            // It is important that used synchronizedMap, because of "synchronized(links) {..}" later in code.
            links = Collections.synchronizedMap(new LinkedHashMap<String, Link>());
        }

        for (Link link : linkSet) {
            String[] pageLinks = link.value().split(";");
            for (String pageLink : pageLinks) {
                if (!sectionsByLinkText.containsKey(pageLink)) {
                    sectionsByLinkText.putIfAbsent(pageLink, parseLinkToLinkSections(pageLink));
                }

                for (Map<String, Link> linkMap : linksByPage.values()) {
                    if (linkMap.containsKey(pageLink)) {
                        throw new ConfigurationException("Page link \"" + pageLink + "\" already registered.");
                    }
                }
                if (links.containsKey(pageLink)) {
                    throw new ConfigurationException("Page link \"" + pageLink + "\" already registered.");
                }

                links.put(pageLink, link);
            }
        }

        linksByPage.put(clazz, links);
    }

    /**
     * @param clazz Page class.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which doesn't use parameters. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLink(Class<? extends Page> clazz) {
        return getLinkByMap(clazz, Collections.<String, Object>emptyMap());
    }

    /**
     * @param name Page name. Use @Name annotation to set page name.
     *             Use simple class name if no @Name used.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which doesn't use parameters. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLink(String name) {
        Class<? extends Page> clazz = classesByName.get(name);

        if (clazz == null) {
            throw new NoSuchLinkException("Can't find link for page \"" + name + "\", " +
                    "because of no such page has been registered.");
        } else {
            return getLink(clazz);
        }
    }

    /**
     * @param clazz  Page class.
     * @param params parameters for substitution (for example link "profile/{handle}"
     *               may use "handle" key in the map.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which matches better. Can throw NoSuchLinkException.
     */
    public static String getLinkByMap(Class<? extends Page> clazz, Map<String, ?> params) {
        Map<String, String> nonEmptyParams = new HashMap<String, String>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (!isMissingValue(value)) {
                nonEmptyParams.put(entry.getKey(), entry.getValue().toString());
            }
        }

        Set<String> linkTexts = linksByPage.get(clazz).keySet();

        int bestMatchedCount = -1;
        List<LinkSection> bestMatchedLinkSections = null;

        for (String linkText : linkTexts) {
            List<LinkSection> sections = sectionsByLinkText.get(linkText);
            boolean matched = true;
            int matchedCount = 0;
            for (LinkSection section : sections) {
                if (section.isParameter()) {
                    matchedCount++;
                    String value = nonEmptyParams.get(section.getParameterName());
                    if (value == null || (!section.getAllowedParameterValues().isEmpty() && !section.getAllowedParameterValues().contains(value))) {
                        matched = false;
                        break;
                    }
                }
            }
            if (matched && matchedCount > bestMatchedCount) {
                bestMatchedCount = matchedCount;
                bestMatchedLinkSections = sections;
            }
        }

        if (bestMatchedLinkSections == null) {
            throw new NoSuchLinkException("Can't find link for page " + clazz.getName() + '.');
        }

        StringBuilder result = new StringBuilder(ApplicationContext.getInstance().getContextPath());
        Set<String> usedKeys = new HashSet<String>();

        for (LinkSection section : bestMatchedLinkSections) {
            String item;

            if (section.isParameter()) {
                usedKeys.add(section.getParameterName());
                item = nonEmptyParams.get(section.getParameterName());
            } else {
                item = section.getValue();
            }

            result.append('/').append(item);
        }

        if (nonEmptyParams.size() > usedKeys.size()) {
            boolean first = true;
            for (Map.Entry<String, String> entry : nonEmptyParams.entrySet()) {
                if (!usedKeys.contains(entry.getKey())) {
                    if (first) {
                        result.append('?');
                        first = false;
                    } else {
                        result.append('&');
                    }
                    result.append(entry.getKey()).append('=').append(entry.getValue());
                }
            }
        }

        return result.toString();
    }

    private static boolean isMissingValue(Object value) {
        return (value == null || value.toString().isEmpty());
    }

    /**
     * @param clazz  Page class.
     * @param params Even length sequence of Objects. Even elements mean keys and odd
     *               values of parameters map. For example ["handle", "MikeMirzayanov", "topic", 123]
     *               means map ["handle" => "MikeMirzayanov", "topic" => 123]. Method skips params with null value.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which matches better. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLink(Class<? extends Page> clazz, Object... params) {
        Map<String, Object> map = convertArrayToMap(params);
        return getLinkByMap(clazz, map);
    }

    /**
     * @param name   Page class.
     * @param params Even length sequence of Objects. Even elements mean keys and odd
     *               values of parameters map. For example ["handle", "MikeMirzayanov", "topic", 123]
     *               means map ["handle" => "MikeMirzayanov", "topic" => 123]. Method skips params with null value.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which matches better. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLink(String name, Object... params) {
        Map<String, Object> map = convertArrayToMap(params);
        return getLinkByMap(name, map);
    }

    /**
     * @param params Array of values.
     * @return Correspondent map.
     */
    private static Map<String, Object> convertArrayToMap(Object... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("Params should contain even number of elements.");
        }

        Map<String, Object> map = new HashMap<String, Object>();

        boolean isKey = true;
        String key = null;
        for (Object param : params) {
            if (isKey) {
                key = param.toString();
            } else {
                map.put(key, param);
            }
            isKey ^= true;
        }
        return map;
    }

    /**
     * @param name   Page name.
     * @param params parameters for substitution (for example link "profile/{handle}"
     *               may use "handle" key in the map.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which matches better. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLinkByMap(String name, Map<String, ?> params) {
        Class<? extends Page> clazz = classesByName.get(name);

        if (clazz == null) {
            throw new NoSuchLinkException("Can't find link for page " + name + '.');
        } else {
            return getLinkByMap(clazz, params);
        }
    }

    /**
     * @param link Relative link to the page started from "/".
     *             For example, "/profile/MikeMirzayanov".
     * @return Result instance or {@code null} if not found.
     */
    public static LinkMatchResult match(String link) {
        // Remove anchor.
        if (link.contains("#")) {
            link = link.substring(0, link.lastIndexOf('#'));
        }

        // Remove query string.
        if (link.contains("?")) {
            link = link.substring(0, link.lastIndexOf('?'));
        }

        if (!link.startsWith("/")) {
            throw new IllegalArgumentException("Link \"" + link + "\" doesn't start with '/'.");
        }

        String[] linkTokens = link.substring(1).split("/");

        for (Map.Entry<Class<? extends Page>, Map<String, Link>> listEntry : linksByPage.entrySet()) {
            final Map<String, Link> patterns = listEntry.getValue();
            if (patterns == null) {
                continue;
            }

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (patterns) {
                for (Map.Entry<String, Link> patternEntry : patterns.entrySet()) {
                    String linkText = patternEntry.getKey();
                    Map<String, String> attrs = match(linkTokens, linkText);

                    if (attrs != null) {
                        return new LinkMatchResult(listEntry.getKey(), linkText, attrs, patternEntry.getValue());
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param linkTokens    For example, ["profile", "MikeMirzayanov"] for requested link "/profile/MikeMirzayanov".
     * @param linkText Link pattern, like "profile/{handle}".
     * @return Correspondent params map or {@code null} if not matched.
     */
    private static Map<String, String> match(String[] linkTokens, String linkText) {
        List<LinkSection> sections = sectionsByLinkText.get(linkText);
        if (sections == null) {
            throw new NocturneException("Can't find sections for linkText=\"" + linkText + "\".");
        }

        if (linkTokens.length != sections.size()) {
            return null;
        } else {
            Map<String, String> attrs = new HashMap<String, String>();

            for (int i = 0; i < linkTokens.length; i++) {
                LinkSection section = sections.get(i);

                if (section.isParameter()) {
                    if (!section.getAllowedParameterValues().isEmpty() && !section.getAllowedParameterValues().contains(linkTokens[i])) {
                        return null;
                    }
                    attrs.put(section.getParameterName(), linkTokens[i]);
                } else {
                    if (!section.getValue().equals(linkTokens[i])) {
                        return null;
                    }
                }
            }

            return attrs;
        }
    }

    /** If case of getLink-like methods if they can't find requested link. */
    public static class NoSuchLinkException extends RuntimeException {
        /** @param message Error message. */
        public NoSuchLinkException(String message) {
            super(message);
        }
    }

    private static List<LinkSection> parseLinkToLinkSections(String linkText) {
        if (linkText == null || linkText.startsWith("/") || linkText.endsWith("/")) {
            throw new ConfigurationException("Page link has illegal format, use links like" +
                    " 'home', 'page/{index}', 'page/{index:1,2,3}'.");
        }

        String[] sections = linkText.split("/");
        List<LinkSection> linkSections = new ArrayList<LinkSection>(sections.length);
        for (String section : sections) {
            linkSections.add(new LinkSection(section));
        }

        return linkSections;
    }

    private static final class LinkSection {
        private final String section;
        private final boolean parameter;
        private final String value;
        private final String parameterName;
        private final Set<String> allowedParameterValues;

        /**
         * @param section Each part of link, i.e. "home" from link "test/home"
         */
        private LinkSection(String section) {
            this.section = section;

            if (section.startsWith("{") && section.endsWith("}")) {
                value = null;
                parameter = true;

                String[] parts = section.substring(1, section.length() - 1).split(":");

                if (parts.length == 1) {
                    parameterName = parts[0];
                    allowedParameterValues = Collections.emptySet();
                } else if (parts.length == 2) {
                    parameterName = parts[0];
                    allowedParameterValues = new ConcurrentSkipListSet<String>(Arrays.asList(parts[1].split(",")));
                } else {
                    throw new ConfigurationException("Link section \"" + section + "\" has invalid format, examples of valid formats: " +
                            "\"test\", \"{userName}\", \"{id:1,2,3}\".");
                }
            } else {
                value = section;
                parameter = false;
                allowedParameterValues = null;
                parameterName = null;
            }
        }

        public boolean isParameter() {
            return parameter;
        }

        public String getValue() {
            if (parameter) {
                throw new IllegalStateException("Can't read value of parameter section \"" + section + "\".");
            }
            return value;
        }

        public String getParameterName() {
            if (!parameter) {
                throw new IllegalStateException("Can't read parameterName of non-parameter section \"" + section + "\".");
            }
            return parameterName;
        }

        public Set<String> getAllowedParameterValues() {
            if (!parameter) {
                throw new IllegalStateException("Can't read allowedParameterValues of non-parameter section \"" + section + "\".");
            }
            return allowedParameterValues;
        }
    }
}
