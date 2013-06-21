/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.link;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateSequenceModel;
import org.apache.commons.lang.mutable.MutableBoolean;
import org.nocturne.annotation.Name;
import org.nocturne.collection.SingleEntryList;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.exception.NocturneException;
import org.nocturne.main.ApplicationContext;
import org.nocturne.main.Page;
import org.nocturne.util.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private static final Lock addLinkLock = new ReentrantLock();

    private static final int INTERCEPTOR_MAX_PERMIT_COUNT = 8 * Runtime.getRuntime().availableProcessors();
    private static final Semaphore interceptorSemaphore = new Semaphore(INTERCEPTOR_MAX_PERMIT_COUNT);
    private static final ConcurrentMap<String, Interceptor> interceptorByNameMap =
            new ConcurrentHashMap<String, Interceptor>();

    /**
     * Stores maps for each page class. Each map contains single patterns as keys
     * and Link instances as values.
     */
    private static final ConcurrentMap<Class<? extends Page>, Map<String, Link>> linksByPage =
            new ConcurrentHashMap<Class<? extends Page>, Map<String, Link>>();

    /**
     * Stores page classes by their names.
     */
    private static final ConcurrentMap<String, Class<? extends Page>> classesByName =
            new ConcurrentHashMap<String, Class<? extends Page>>();

    /**
     * Stores link sections by links.
     */
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

    /**
     * Use the method to get page link name. Do not use page.getClass().getSimpleName() because of two reasons:
     * - page can have @Name annotation,
     * - page can be actually inherited from expected ConcretePage class because of IoC.
     *
     * @param page Page instance.
     * @return Page link name.
     */
    public static String getLinkName(@Nonnull Page page) {
        return getLinkName(page.getClass());
    }

    /**
     * Use the method to get page class link name. Do not use pageClass.getSimpleName() because of two reasons:
     * - page class can have @Name annotation,
     * - page class can be actually inherited from expected ConcretePage class because of IoC.
     *
     * @param pageClass Page class.
     * @return Page link name.
     */
    public static String getLinkName(@Nonnull Class<? extends Page> pageClass) {
        Class<?> clazz = pageClass;

        while (clazz != null && clazz.getAnnotation(Link.class) == null && clazz.getAnnotation(LinkSet.class) == null) {
            clazz = clazz.getSuperclass();
        }

        if (clazz == null) {
            throw new NocturneException("Page class should have @Link or @LinkSet annotation, but "
                    + pageClass.getName() + " hasn't.");
        }

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
    public static void add(Class<? extends Page> clazz) {
        addLinkLock.lock();

        try {
            List<Link> linkSet = getLinksViaReflection(clazz);
            if (linkSet.isEmpty()) {
                throw new ConfigurationException("Can't find link for page " + clazz.getName() + '.');
            }

            String name = getLinkName(clazz);
            if (classesByName.containsKey(name) && !clazz.equals(classesByName.get(name))) {
                throw new ConfigurationException("Can't add page which is not unique by it's name: "
                        + clazz.getName() + '.');
            }
            classesByName.put(name, clazz);

            Map<String, Link> links = getLinksByPageClass(clazz);
            if (links == null) {
                // It is important that used synchronizedMap, because of "synchronized(links) {..}" later in code.
                links = Collections.synchronizedMap(new LinkedHashMap<String, Link>());
            }

            for (Link link : linkSet) {
                String[] pageLinks = StringUtil.Patterns.SEMICOLON_PATTERN.split(link.value());
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
        } finally {
            addLinkLock.unlock();
        }
    }

    /**
     * @param clazz    Page class.
     * @param linkName desired {@link Link#name() name} of the link
     * @param params   parameters for substitution (for example link "profile/{handle}"
     *                 may use "handle" key in the map.
     * @return link for page. If there many links for page, returns one of them, which matches better
     * @throws NoSuchLinkException if no such link exists
     */
    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    public static String getLinkByMap(Class<? extends Page> clazz, @Nullable String linkName, Map<String, ?> params) {
        MutableBoolean multiValueParams = new MutableBoolean();
        Map<String, List<String>> nonEmptyParams = getNonEmptyParams(params, multiValueParams);

        int bestMatchedCount = -1;
        List<LinkSection> bestMatchedLinkSections = null;

        for (Map.Entry<String, Link> entry : getLinksByPageClass(clazz).entrySet()) {
            if (linkName != null && !linkName.isEmpty() && !linkName.equals(entry.getValue().name())) {
                continue;
            }

            List<LinkSection> sections = sectionsByLinkText.get(entry.getKey());
            boolean matched = true;
            int matchedCount = 0;
            for (LinkSection section : sections) {
                if (section.isParameter()) {
                    ++matchedCount;
                    List<String> values = nonEmptyParams.get(section.getParameterName());
                    String value = values == null ? null : values.get(0);
                    Set<String> allowedParameterValues = section.getAllowedParameterValues();
                    if (value == null
                            || (!allowedParameterValues.isEmpty() && !allowedParameterValues.contains(value))) {
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
            if (linkName == null || linkName.isEmpty()) {
                throw new NoSuchLinkException("Can't find link for page " + clazz.getName() + '.');
            } else {
                throw new NoSuchLinkException(
                        "Can't find link with name \'" + linkName + "\' for page " + clazz.getName() + '.'
                );
            }
        }

        StringBuilder result = new StringBuilder(ApplicationContext.getInstance().getContextPath());
        Set<String> usedKeys = new HashSet<String>();

        for (LinkSection section : bestMatchedLinkSections) {
            String item;

            if (section.isParameter()) {
                usedKeys.add(section.getParameterName());
                item = nonEmptyParams.get(section.getParameterName()).get(0);
            } else {
                item = section.getValue();
            }

            result.append('/').append(item);
        }

        if (nonEmptyParams.size() > usedKeys.size() || multiValueParams.isTrue()) {
            boolean first = true;
            for (Map.Entry<String, List<String>> entry : nonEmptyParams.entrySet()) {
                List<String> values = entry.getValue();
                int valueCount = values.size();
                int startIndex = valueCount;

                if (usedKeys.contains(entry.getKey())) {
                    if (valueCount > 1) {
                        startIndex = 1;
                    }
                } else {
                    startIndex = 0;
                }

                for (int i = startIndex; i < valueCount; ++i) {
                    if (first) {
                        result.append('?');
                        first = false;
                    } else {
                        result.append('&');
                    }

                    result.append(entry.getKey()).append('=').append(values.get(i));
                }
            }
        }

        String linkResult = result.toString();

        interceptorSemaphore.acquireUninterruptibly();
        try {
            for (Interceptor interceptor : interceptorByNameMap.values()) {
                linkResult = interceptor.postprocess(linkResult, clazz, linkName, params);
            }
        } finally {
            interceptorSemaphore.release();
        }

        return linkResult;
    }

    private static Map<String, List<String>> getNonEmptyParams(Map<String, ?> params, MutableBoolean multiValueParams) {
        multiValueParams.setValue(false);
        Map<String, List<String>> nonEmptyParams = new LinkedHashMap<String, List<String>>();

        for (Map.Entry<String, ?> entry : params.entrySet()) {
            Object value = entry.getValue();
            if (!isMissingValue(value)) {
                List<String> list = toStringList(value);
                int count = list.size();

                if (count > 0) {
                    nonEmptyParams.put(entry.getKey(), list);

                    if (count > 1) {
                        multiValueParams.setValue(true);
                    }
                }
            }
        }

        return nonEmptyParams;
    }

    @Nonnull
    private static List<String> toStringList(@Nonnull Object value) {
        if (value instanceof TemplateSequenceModel) {
            return toStringList((TemplateSequenceModel) value);
        } else if (value instanceof Collection) {
            return toStringList((Collection) value);
        } else if (value.getClass().isArray()) {
            int count = Array.getLength(value);
            List<String> list = new ArrayList<String>(count);

            for (int i = 0; i < count; ++i) {
                Object item = Array.get(value, i);
                if (item != null) {
                    list.add(item.toString());
                }
            }

            return list;
        } else {
            return new SingleEntryList<String>(value.toString());
        }
    }

    private static List<String> toStringList(@Nonnull TemplateSequenceModel sequence) {
        int count = getSize(sequence);
        List<String> list = new ArrayList<String>(count);

        for (int i = 0; i < count; ++i) {
            TemplateModel item;
            try {
                item = sequence.get(i);
            } catch (TemplateModelException e) {
                throw new NocturneException("Can't get item of Freemarker sequence.", e);
            }

            if (item != null) {
                list.add(item.toString());
            }
        }

        return list;
    }

    private static List<String> toStringList(@Nonnull Collection collection) {
        List<String> list = new ArrayList<String>(collection.size());

        for (Object item : collection) {
            if (item != null) {
                list.add(item.toString());
            }
        }

        return list;
    }

    private static int getSize(@Nonnull TemplateSequenceModel sequence) {
        try {
            return sequence.size();
        } catch (TemplateModelException e) {
            throw new NocturneException("Can't get size of Freemarker sequence.", e);
        }
    }

    /**
     * @param clazz  Page class.
     * @param params parameters for substitution (for example link "profile/{handle}"
     *               may use "handle" key in the map.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which matches better. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLinkByMap(Class<? extends Page> clazz, Map<String, ?> params) {
        return getLinkByMap(clazz, null, params);
    }

    /**
     * @param name     Page name.
     * @param linkName desired {@link Link#name() name} of the link
     * @param params   parameters for substitution (for example link "profile/{handle}"
     *                 may use "handle" key in the map.
     * @return Returns link for page. If there many links for page, returns
     *         one of them, which matches better. Throws NoSuchLinkException
     *         if no such link exists.
     */
    public static String getLinkByMap(String name, @Nullable String linkName, Map<String, ?> params) {
        Class<? extends Page> clazz = classesByName.get(name);

        if (clazz == null) {
            throw new NoSuchLinkException("Can't find link for page " + name + '.');
        } else {
            return getLinkByMap(clazz, linkName, params);
        }
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
        return getLinkByMap(name, null, params);
    }

    private static boolean isMissingValue(Object value) {
        if (value == null) {
            return true;
        }

        if (value instanceof TemplateSequenceModel) {
            return getSize((TemplateSequenceModel) value) <= 0;
        } else if (value instanceof Collection) {
            return ((Collection) value).isEmpty();
        } else if (value.getClass().isArray()) {
            return Array.getLength(value) <= 0;
        } else {
            return value.toString().isEmpty();
        }
    }

    /**
     * @param params Array of values.
     * @return Correspondent map.
     */
    private static Map<String, Object> convertArrayToMap(Object... params) {
        int paramCount = params.length;

        if (paramCount == 0) {
            return Collections.emptyMap();
        }

        if (paramCount % 2 != 0) {
            throw new IllegalArgumentException("Params should contain even number of elements.");
        }

        Map<String, Object> map = new LinkedHashMap<String, Object>();

        for (int paramIndex = 0; paramIndex < paramCount; paramIndex += 2) {
            map.put(params[paramIndex].toString(), params[paramIndex + 1]);
        }

        return map;
    }

    /**
     * @param pageClass Page class.
     * @return link for page. If there many links for page, returns one of them, which matches better
     * @throws NoSuchLinkException if no such link exists
     */
    public static String getLink(Class<? extends Page> pageClass) {
        return getLinkByMap(pageClass, null, Collections.<String, Object>emptyMap());
    }

    /**
     * @param pageClass Page class.
     * @param params    Even length sequence of Objects. Even elements mean keys and odd
     *                  values of parameters map. For example ["handle", "MikeMirzayanov", "topic", 123]
     *                  means map ["handle" => "MikeMirzayanov", "topic" => 123]. Method skips params with null value.
     * @return link for page. If there many links for page, returns one of them, which matches better
     * @throws NoSuchLinkException if no such link exists
     */
    public static String getLink(Class<? extends Page> pageClass, Object... params) {
        return getLinkByMap(pageClass, null, convertArrayToMap(params));
    }

    /**
     * @param name   Page name.
     * @param params Even length sequence of Objects. Even elements mean keys and odd
     *               values of parameters map. For example ["handle", "MikeMirzayanov", "topic", 123]
     *               means map ["handle" => "MikeMirzayanov", "topic" => 123]. Method skips params with null value.
     * @return link for page. If there many links for page, returns one of them, which matches better
     * @throws NoSuchLinkException if no such link exists
     */
    public static String getLink(String name, Object... params) {
        return getLinkByMap(name, null, convertArrayToMap(params));
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

        String[] linkTokens = StringUtil.Patterns.SLASH_PATTERN.split(link.substring(1));

        for (Map.Entry<Class<? extends Page>, Map<String, Link>> listEntry : linksByPage.entrySet()) {
            Map<String, Link> patterns = listEntry.getValue();
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
     * @param linkTokens For example, ["profile", "MikeMirzayanov"] for requested link "/profile/MikeMirzayanov".
     * @param linkText   Link pattern, like "profile/{handle}".
     * @return Correspondent params map or {@code null} if not matched.
     */
    private static Map<String, String> match(String[] linkTokens, String linkText) {
        List<LinkSection> sections = sectionsByLinkText.get(linkText);
        if (sections == null) {
            throw new NocturneException("Can't find sections for linkText=\"" + linkText + "\".");
        }

        int linkTokenCount = linkTokens.length;

        if (linkTokenCount == sections.size()) {
            Map<String, String> attrs = new HashMap<String, String>();

            for (int linkTokenIndex = 0; linkTokenIndex < linkTokenCount; ++linkTokenIndex) {
                LinkSection section = sections.get(linkTokenIndex);

                if (section.isParameter()) {
                    if (!section.getAllowedParameterValues().isEmpty()
                            && !section.getAllowedParameterValues().contains(linkTokens[linkTokenIndex])) {
                        return null;
                    }
                    attrs.put(section.getParameterName(), linkTokens[linkTokenIndex]);
                } else {
                    if (!section.getValue().equals(linkTokens[linkTokenIndex])) {
                        return null;
                    }
                }
            }

            return attrs;
        } else {
            return null;
        }
    }

    /**
     * If case of getLink-like methods if they can't find requested link.
     */
    @SuppressWarnings({"DeserializableClassInSecureContext", "UncheckedExceptionClass"})
    public static class NoSuchLinkException extends RuntimeException {
        /**
         * @param message Error message.
         */
        public NoSuchLinkException(String message) {
            super(message);
        }
    }

    private static List<LinkSection> parseLinkToLinkSections(String linkText) {
        if (linkText == null || linkText.startsWith("/") || linkText.endsWith("/")) {
            throw new ConfigurationException("Page link has illegal format, use links like" +
                    " 'home', 'page/{index}', 'page/{index:1,2,3}'.");
        }

        String[] sections = StringUtil.Patterns.SLASH_PATTERN.split(linkText);
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

                String[] parts = StringUtil.Patterns.COLON_PATTERN.split(section.substring(1, section.length() - 1));

                if (parts.length == 1) {
                    parameterName = parts[0];
                    allowedParameterValues = Collections.emptySet();
                } else if (parts.length == 2) {
                    parameterName = parts[0];
                    allowedParameterValues = new ConcurrentSkipListSet<String>(
                            Arrays.asList(StringUtil.Patterns.COMMA_PATTERN.split(parts[1]))
                    );
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
            return Collections.unmodifiableSet(allowedParameterValues);
        }
    }

    /**
     * Adds interceptor to the Links. Link will be processed by interceptors before return.
     *
     * @param name        name of the interceptor to add
     * @param interceptor interceptor to add
     */
    public static void addInterceptor(String name, Interceptor interceptor) {
        ensureInterceptorName(name);

        if (interceptor == null) {
            throw new IllegalArgumentException("Argument \'interceptor\' is \'null\'.");
        }

        interceptorSemaphore.acquireUninterruptibly(INTERCEPTOR_MAX_PERMIT_COUNT);
        try {
            if (interceptorByNameMap.containsKey(name)) {
                throw new IllegalStateException("Interceptor with name \'" + name + "\' already added.");
            }
            interceptorByNameMap.put(name, interceptor);
        } finally {
            interceptorSemaphore.release(INTERCEPTOR_MAX_PERMIT_COUNT);
        }
    }

    /**
     * Removes interceptor from the Links.
     *
     * @param name name of the interceptor to remove
     */
    public static void removeInterceptor(String name) {
        ensureInterceptorName(name);

        interceptorSemaphore.acquireUninterruptibly(INTERCEPTOR_MAX_PERMIT_COUNT);
        try {
            interceptorByNameMap.remove(name);
        } finally {
            interceptorSemaphore.release(INTERCEPTOR_MAX_PERMIT_COUNT);
        }
    }

    /**
     * Checks if specified interceptor is already added to the Links.
     *
     * @param name name of the interceptor to check
     * @return {@code true} iff interceptor with specified name is added to the Links
     */
    public static boolean hasInterceptor(String name) {
        ensureInterceptorName(name);

        interceptorSemaphore.acquireUninterruptibly(INTERCEPTOR_MAX_PERMIT_COUNT);
        try {
            return interceptorByNameMap.containsKey(name);
        } finally {
            interceptorSemaphore.release(INTERCEPTOR_MAX_PERMIT_COUNT);
        }
    }

    private static void ensureInterceptorName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Argument \'name\' is \'null\' or empty.");
        }
    }

    private static Map<String, Link> getLinksByPageClass(Class<? extends Page> clazz) {
        Map<String, Link> links;
        Class parentClass = clazz;
        while ((links = linksByPage.get(parentClass)) == null && parentClass.getSuperclass() != null) {
            parentClass = parentClass.getSuperclass();
        }
        return links;
    }

    /**
     * Custom link processor. You can add interceptor using {@link #addInterceptor(String, Interceptor)} method.
     */
    public interface Interceptor {
        /**
         * This method will be called to postprocess link.
         *
         * @param link     link to process
         * @param clazz    page class
         * @param linkName {@link Link#name() name} of the link
         * @param params   parameters of the link
         * @return processed link
         */
        String postprocess(String link, Class<? extends Page> clazz, @Nullable String linkName, Map<String, ?> params);
    }
}
