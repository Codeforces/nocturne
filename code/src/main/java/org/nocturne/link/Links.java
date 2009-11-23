/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.link;

import org.nocturne.exception.ConfigurationException;
import org.nocturne.main.ApplicationContext;
import org.nocturne.annotation.Name;
import org.nocturne.main.Page;

import java.util.*;

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
            Collections.synchronizedMap(new HashMap<Class<? extends Page>, Map<String, Link>>());

    /** Stores page classes by their names. */
    private static final Map<String, Class<? extends Page>> classesByName =
            Collections.synchronizedMap(new HashMap<String, Class<? extends Page>>());

    /** Stores parameter names in pattern (page link). */
    private static final Map<String, Set<String>> parametersByLink =
            Collections.synchronizedMap(new HashMap<String, Set<String>>());

    /** As ZERO. */
    private static final Set<String> EMPTY_STRING_SET = Collections.emptySet();

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
     *              After it you can get it's link via getLink, or using @link directiive
     *              from template. Link may contain template sections, like "profile/{handle}".
     */
    public static synchronized void add(Class<? extends Page> clazz) {
        List<Link> linkSet = getLinksViaReflection(clazz);
        if (linkSet.isEmpty()) {
            throw new ConfigurationException("Can't find link for page " + clazz.getName() + ".");
        }

        String name = getNameViaReflection(clazz);
        if (classesByName.containsKey(name) && !clazz.equals(classesByName.get(name))) {
            throw new IllegalArgumentException("Can't add page which is not unique by it's name: "
                    + clazz.getName() + ".");
        }
        classesByName.put(name, clazz);

        Map<String, Link> links = linksByPage.get(clazz);
        if (links == null) {
            links = Collections.synchronizedMap(new LinkedHashMap<String, Link>());
        }

        for (Link link : linkSet) {
            String[] pageLinks = link.value().split(";");
            for (String pageLink : pageLinks) {
                if (!parametersByLink.containsKey(pageLink)) {
                    setupParametersByLink(pageLink);
                }
            }

            for (String pageLink : pageLinks) {
                links.put(pageLink, link);
            }
        }

        linksByPage.put(clazz, links);
    }

    private static void setupParametersByLink(String pageLink) {
        Set<String> params = new HashSet<String>();

        StringBuilder sb = new StringBuilder();
        boolean started = false;

        for (int i = 0; i < pageLink.length(); i++) {
            if (pageLink.charAt(i) == '{') {
                sb = new StringBuilder();
                started = true;
                continue;
            }
            if (pageLink.charAt(i) == '}') {
                params.add(sb.toString());
                started = false;
                continue;
            }
            if (started) {
                sb.append(pageLink.charAt(i));
            }
        }

        parametersByLink.put(pageLink, params);
    }

    /**
     * @param clazz  Page class.
     * @param params Set of parameter names.
     * @return Correspondent page link (pattern).
     */
    private static String getLinkByPageAndParams(Class<? extends Page> clazz, Set<String> params) {
        Map<String, Link> linksMap = linksByPage.get(clazz);
        if (linksMap == null) {
            throw new NoSuchLinkException("Can't find link for page " + clazz.getName() + ".");
        }

        Set<String> links = linksMap.keySet();

        String link = null;
        int maxMathedCount = -1;

        for (String linkItem : links) {
            int matchedCount = 0;
            Set<String> pageParams = parametersByLink.get(linkItem);

            for (String pageParam : pageParams) {
                if (!params.contains(pageParam)) {
                    matchedCount = Integer.MIN_VALUE;
                }
            }

            if (matchedCount == 0) {
                for (String param : params) {
                    if (pageParams.contains(param)) {
                        matchedCount++;
                    }
                }
            }

            if (matchedCount > maxMathedCount) {
                link = linkItem;
                maxMathedCount = matchedCount;
            }
        }

        if (link == null) {
            throw new NoSuchLinkException("Can't find link for page " + clazz.getName() + ".");
        }

        if (link.startsWith("/")) {
            return link;
        } else {
            return ApplicationContext.getInstance().getRequest().getContextPath() + "/" + link;
        }
    }

    /**
     * @param clazz Page class.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which doesn't use parameters. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLink(Class<? extends Page> clazz) {
        return getLinkByPageAndParams(clazz, EMPTY_STRING_SET);
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
            return null;
        } else {
            return getLinkByPageAndParams(clazz, EMPTY_STRING_SET);
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
        Set<String> paramSet = new HashSet<String>();
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (!isMissingValue(value)) {
                paramSet.add(entry.getKey());
            }
        }

        String link = getLinkByPageAndParams(clazz, paramSet);

        StringBuilder sb = new StringBuilder();
        Set<String> usedKeys = new HashSet<String>();

        if (link.indexOf('{') >= 0) {
            String[] tokens = link.split("/");

            boolean first = true;
            for (String token : tokens) {
                if (!first) {
                    sb.append("/");
                } else {
                    first = false;
                }
                if (token.startsWith("{") && token.endsWith("}")) {
                    String key = token.substring(1, token.length() - 1);
                    sb.append(params.get(key).toString());
                    usedKeys.add(key);
                } else {
                    sb.append(token);
                }
            }
        } else {
            sb = new StringBuilder(link);
        }

        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (!usedKeys.contains(entry.getKey()) && !isMissingValue(entry.getValue())) {
                if (query.length() == 0) {
                    query.append("?");
                } else {
                    query.append("&");
                }
                query.append(entry.getKey()).append("=").append(entry.getValue().toString());
            }
        }

        if (query.length() == 0) {
            return sb.toString();
        } else {
            return sb.append(query).toString();
        }
    }

    private static boolean isMissingValue(Object value) {
        return (value == null || "".equals(value.toString()));
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
            throw new NoSuchLinkException("Can't find link for page " + name + ".");
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
        for (Map.Entry<Class<? extends Page>, Map<String, Link>> listEntry : linksByPage.entrySet()) {
            Map<String, Link> patterns = listEntry.getValue();

            if (patterns == null) {
                continue;
            }

            synchronized (patterns) {
                for (Map.Entry<String, Link> patternEntry : patterns.entrySet()) {
                    String pattern = patternEntry.getKey();
                    Map<String, String> attrs = match(link, pattern);

                    if (attrs != null) {
                        return new LinkMatchResult(listEntry.getKey(), pattern, attrs, patternEntry.getValue());
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param link    Relative link to the page started from "/".
     *                For example, "/profile/MikeMirzayanov".
     * @param pattern Link pattern, like "profile/{handle}".
     * @return Correspondent params map or {@code null} if not matched.
     */
    public static Map<String, String> match(String link, String pattern) {
        // Remove anchor.
        if (link.contains("#")) {
            link = link.substring(0, link.lastIndexOf('#'));
        }

        // Remove query string.
        if (link.contains("?")) {
            link = link.substring(0, link.lastIndexOf('?'));
        }

        pattern = "/" + pattern;

        String[] linkTokens = link.split("/");
        String[] patternTokens = pattern.split("/");

        if (linkTokens.length != patternTokens.length) {
            return null;
        } else {
            Map<String, String> attrs = new HashMap<String, String>();

            for (int i = 0; i < patternTokens.length; i++) {
                if (patternTokens[i].startsWith("{") && patternTokens[i].endsWith("}")) {
                    attrs.put(patternTokens[i].substring(1, patternTokens[i].length() - 1), linkTokens[i]);
                    continue;
                }

                if (!patternTokens[i].equals(linkTokens[i])) {
                    return null;
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
}
