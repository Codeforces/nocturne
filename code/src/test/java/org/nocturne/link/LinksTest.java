package org.nocturne.link;

import junit.framework.TestCase;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.link.pages.*;
import org.nocturne.main.ApplicationContextHelper;
import org.nocturne.main.Page;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Mirzayanov
 */
public class LinksTest extends TestCase {
    private static final String CONTEXT_PATH = "http://code.google.com/nocturne";

    static {
        ApplicationContextHelper.setContextPath(CONTEXT_PATH);

        Links.add(IndexPage.class);
        Links.add(LongLinkedPage.class);
        Links.add(NewsPage.class);
        Links.add(ProfilePage.class);
        Links.add(SectionsPage.class);
    }

    public void testIndexPage() {
        internalTestIndexPageWithoutInterceptors();

        Links.addInterceptor("testInterceptor", new Links.Interceptor() {
            @Override
            public String postprocess(
                    String link, Class<? extends Page> clazz, @Nullable String linkName, Map<String, ?> params) {
                return link.replace("vip", "test");
            }
        });

        assertEquals(CONTEXT_PATH + '/', Links.getLink("IndexPage"));
        assertEquals(CONTEXT_PATH + "/page/17", Links.getLink("IndexPage", "pageIndex", 17));
        assertEquals(CONTEXT_PATH + "/page/17?i=3", Links.getLink(IndexPage.class, "pageIndex", 17, "i", 3));
        assertEquals(CONTEXT_PATH + '/', Links.getLinkByMap(IndexPage.class, null, Collections.<String, Object>emptyMap()));
        assertEquals(CONTEXT_PATH + "/testIndex", Links.getLinkByMap(IndexPage.class, "vipLink", Collections.<String, Object>emptyMap()));

        Links.removeInterceptor("testInterceptor");

        internalTestIndexPageWithoutInterceptors();
    }

    private static void internalTestIndexPageWithoutInterceptors() {
        assertEquals(CONTEXT_PATH + '/', Links.getLink("IndexPage"));
        assertEquals(CONTEXT_PATH + "/page/17", Links.getLink("IndexPage", "pageIndex", 17));
        assertEquals(CONTEXT_PATH + "/page/17?i=3", Links.getLink(IndexPage.class, "pageIndex", 17, "i", 3));
        assertEquals(CONTEXT_PATH + '/', Links.getLinkByMap(IndexPage.class, null, Collections.<String, Object>emptyMap()));
        assertEquals(CONTEXT_PATH + "/vipIndex", Links.getLinkByMap(IndexPage.class, "vipLink", Collections.<String, Object>emptyMap()));

        assertEqualsLinkMatchResultWithName(Links.match("/index"), IndexPage.class, "index", null);
        assertEqualsLinkMatchResultWithName(Links.match("/index?a=b"), IndexPage.class, "index", null);
        assertEqualsLinkMatchResultWithName(Links.match("/index/a"), null, null, null);
        assertEqualsLinkMatchResultWithName(Links.match("/"), IndexPage.class, "", null);
        assertEqualsLinkMatchResultWithName(Links.match("/page/17"), IndexPage.class, "page/{pageIndex}", null, "pageIndex", "17");
        assertEqualsLinkMatchResultWithName(Links.match("/index/page/17"), IndexPage.class, "index/page/{pageIndex}", null, "pageIndex", "17");
        assertEqualsLinkMatchResultWithName(Links.match("/vipIndex"), IndexPage.class, "vipIndex", "vipLink");
        assertEqualsLinkMatchResultWithName(Links.match("/vipPage/1"), IndexPage.class, "vipPage/{pageIndex}", "vipLink", "pageIndex", "1");
    }

    public void testLongLinkedPage() {
        assertThrows(new Invokable() {
            @Override
            public void invoke() {
                Links.getLink("LongLinkedPage");
            }
        }, Links.NoSuchLinkException.class);

        assertEquals(CONTEXT_PATH + "/LongLinkedPage/a/1/b/2/c/3", Links.getLink("LongLinkedPage", "a", 1, "b", 2, "c", 3L));
        assertEquals(CONTEXT_PATH + "/LongLinkedPage/a/1/b/2/c/3?cc=cc2", Links.getLink("LongLinkedPage", "b", 2, "a", 1, "cc", "cc2", "c", 3L));
        assertEquals(
                CONTEXT_PATH + "/LongLinkedPage/a/aa/b/bb/c/cc?a=aaa&a=4a&c=ccc&c=4c&c=5c&d=-3&d=-2&d=-1",
                Links.getLink(
                        "LongLinkedPage",
                        "a", new String[]{"aa", "aaa", "4a"},
                        "b", "bb",
                        "c", Arrays.<CharSequence>asList("cc", "ccc", "4c", "5c"),
                        "d", new byte[]{-3, -2, -1}
                )
        );
        assertEqualsLinkMatchResult(Links.match("/LongLinkedPage/a/1/b/2/c/3"), LongLinkedPage.class, "LongLinkedPage/a/{a}/b/{b}/c/{c}", "a", "1", "b", "2", "c", "3");
    }

    public void testNewsPage() {
        assertThrows(new Invokable() {
            @Override
            public void invoke() {
                Links.getLink(" news");
            }
        }, Links.NoSuchLinkException.class);

        assertEquals(CONTEXT_PATH + "/news", Links.getLink(NewsPage.class));
        assertEqualsLinkMatchResult(Links.match("/news#12"), NewsPage.class, "news");
        assertEqualsLinkMatchResult(Links.match("/news?a=1&v=2233"), NewsPage.class, "news");
    }

    public void testProfilePage() {
        assertEquals(CONTEXT_PATH + "/profile/Mike", Links.getLink(ProfilePage.class, "userName", "Mike"));
        assertEquals(CONTEXT_PATH + "/profiles/all", Links.getLink(ProfilePage.class, "userName", ""));
        assertEquals(CONTEXT_PATH + "/profiles/all", Links.getLink(ProfilePage.class, "userName", null));
        assertEqualsLinkMatchResult(Links.match("/profile/Mike"), ProfilePage.class, "profile/{userName:Mike,Max}", "userName", "Mike");
        assertEqualsLinkMatchResult(Links.match("/profile/Max"), ProfilePage.class, "profile/{userName:Mike,Max}", "userName", "Max");
        assertEqualsLinkMatchResult(Links.match("/profile/MikeMax"), null, null);
        assertEqualsLinkMatchResult(Links.match("/profile/%20Mike"), null, null);
        assertEqualsLinkMatchResult(Links.match("/profile/ Mike "), null, null);
        assertEqualsLinkMatchResult(Links.match("/profiles/all"), ProfilePage.class, "profiles/all");
        assertEqualsLinkMatchResult(Links.match("/profiles/all/Max"), null, null);
        assertEqualsLinkMatchResult(Links.match("/profiles"), null, null);
        assertEqualsLinkMatchResult(Links.match("/profile"), null, null);
        assertEqualsLinkMatchResult(Links.match("/profile/Mike/Mirzayanov"), null, null);
    }

    public void testSectionsPage() {
        assertThrows(new Invokable() {
            @Override
            public void invoke() {
                Links.getLink(SectionsPage.class, "sectionId", "11");
            }
        }, Links.NoSuchLinkException.class);

        assertEquals(CONTEXT_PATH + "/sections/01", Links.getLink(SectionsPage.class, "sectionId", "01"));
        assertEquals(CONTEXT_PATH + "/sections/11", Links.getLink(SectionsPage.class, "sectionName", "11"));

        assertEquals("SectionsPageBySectionId", Links.match("/sections/01").getLink().name());
        assertEquals("SectionsPageBySectionName", Links.match("/sections/11").getLink().name());
    }

    public void testOneMoreIndexPage() {
        assertThrows(new Invokable() {
            @Override
            public void invoke() {
                Links.add(OneMoreIndexPage.class);
            }
        }, ConfigurationException.class);
    }

    private static void assertEqualsLinkMatchResult(
            LinkMatchResult linkMatchResult, @Nullable Class<? extends Page> clazz, @Nullable String pattern,
            String... attributes) {
        if (clazz == null) {
            assertNull(linkMatchResult);
            return;
        }

        assertEquals(clazz, linkMatchResult.getPageClass());
        assertEquals(pattern, linkMatchResult.getPattern());
        assertEquals(convertArrayToMap(attributes), linkMatchResult.getAttributes());
    }

    private static void assertEqualsLinkMatchResultWithName(
            LinkMatchResult linkMatchResult, @Nullable Class<? extends Page> clazz, @Nullable String pattern,
            @Nullable String linkName, String... attributes) {
        if (clazz == null) {
            assertNull(linkMatchResult);
            return;
        }

        assertEquals(clazz, linkMatchResult.getPageClass());
        assertEquals(pattern, linkMatchResult.getPattern());
        assertEquals(convertArrayToMap(attributes), linkMatchResult.getAttributes());
        assertEquals(
                linkName == null ? "" : linkName,
                linkMatchResult.getLink().name() == null ? "" : linkMatchResult.getLink().name()
        );
    }

    private static Map<String, String> convertArrayToMap(String... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("Params should contain even number of elements.");
        }

        Map<String, String> map = new HashMap<>();

        boolean isKey = true;
        String key = null;
        for (String param : params) {
            if (isKey) {
                key = param;
            } else {
                map.put(key, param);
            }
            isKey ^= true;
        }
        return map;
    }

    private static void assertThrows(Invokable invokable, Class<? extends Throwable> throwableClass) {
        Throwable throwable = null;

        try {
            invokable.invoke();
        } catch (Throwable e) {
            throwable = e;
        }

        assertNotNull(throwable);
        assertEquals(throwableClass, throwable.getClass());
    }

    private interface Invokable {
        void invoke() throws Throwable;
    }
}
