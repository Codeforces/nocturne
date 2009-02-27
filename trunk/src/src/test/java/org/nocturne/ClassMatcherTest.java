package org.nocturne;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.nocturne.misc.ClassMatcher;

/** @author Mike Mirzayanov */
public class ClassMatcherTest extends TestCase {
    public static Test suite() {
        return new TestSuite(ClassMatcherTest.class);
    }

    public void testOneTerm() {
        ClassMatcher matcher = new ClassMatcher("+java.lang.String");
        assertTrue(matcher.match("java.lang.String"));
        assertFalse(matcher.match("java.lang.StringBuffer"));

        matcher = new ClassMatcher("+java.lang");
        assertTrue(matcher.match("java.lang.String"));
        assertTrue(matcher.match("java.lang.StringBuffer"));
        assertFalse(matcher.match("java.regex.Pattern"));
        assertFalse(matcher.match("javax.servlet.HttpServlet"));
    }

    public void testOr() {
        ClassMatcher matcher = new ClassMatcher("+java.lang.String OR +java.lang.Integer");
        assertTrue(matcher.match("java.lang.String"));
        assertTrue(matcher.match("java.lang.Integer"));
        assertFalse(matcher.match("java.lang.StringInteger"));
        assertFalse(matcher.match("java.lang.IntegerInteger"));
        assertFalse(matcher.match("java.String"));
        assertFalse(matcher.match("Integer"));

        matcher = new ClassMatcher("  +java.lang.String    OR    +java.lang.Integer OR     +java.lang.Double \n\n\n\t ");
        assertTrue(matcher.match("java.lang.String"));
        assertTrue(matcher.match("java.lang.Integer"));
        assertTrue(matcher.match("java.lang.Double"));
        assertFalse(matcher.match("java.lang.StringInteger"));
        assertFalse(matcher.match("java.lang.IntegerInteger"));
        assertFalse(matcher.match("java.String"));
        assertFalse(matcher.match("lang.Double"));
        assertFalse(matcher.match("Integer"));
    }

    public void testAnd() {
        ClassMatcher matcher = new ClassMatcher("+java.lang.String AND    +java.lang.Integer  ");
        assertFalse(matcher.match("java.lang.String"));
        assertFalse(matcher.match("java.lang.Integer"));
        assertFalse(matcher.match("java.lang.StringInteger"));
        assertFalse(matcher.match("java.lang.IntegerInteger"));
        assertFalse(matcher.match("java.String"));
        assertFalse(matcher.match("Integer"));
    }

    public void testMinus() {
        ClassMatcher matcher = new ClassMatcher("-java.lang");
        assertFalse(matcher.match("java.lang.String"));
        assertFalse(matcher.match("java.lang.Double"));
        assertFalse(matcher.match("java.lang.internal.Class"));
        assertTrue(matcher.match("java.Lang"));
        assertTrue(matcher.match("javax.lang.String"));

        matcher = new ClassMatcher("-java.lang AND -javax.lang");
        assertFalse(matcher.match("java.lang.String"));
        assertFalse(matcher.match("java.lang.Double"));
        assertFalse(matcher.match("java.lang.internal.Class"));
        assertTrue(matcher.match("java.Lang"));
        assertFalse(matcher.match("javax.lang.String"));
        assertFalse(matcher.match("javax.lang.java.lang.internal.Class"));
    }

    public void testBrackets() {
        ClassMatcher matcher = new ClassMatcher("( +java.lang OR +java.util  ) AND  ( -java.lang.internal AND  -java.util.List) ");
        assertTrue(matcher.match("java.lang.String"));
        assertTrue(matcher.match("java.util.ArrayList"));
        assertFalse(matcher.match("java.lang.internal.Class"));
        assertFalse(matcher.match("java.util.List"));
    }

    public void testInnerClasses() {
        ClassMatcher matcher = new ClassMatcher("(+java.lang");
        assertTrue(matcher.match("java.lang.String"));
        assertTrue(matcher.match("java.lang.String$1"));

        matcher = new ClassMatcher("(+java.lang.String");
        assertTrue(matcher.match("java.lang.String"));
        assertTrue(matcher.match("java.lang.String$1"));
    }

    public void testIllegalSyntax() {
        String pattern = "(+polygon AND -polygon.database.DataSourceFactory AND -polygon.dao.misc.RepositoryResources AND -polygon.compilation.database.DatabaseResources -polygon.compilation.database.DataSourceFactory) OR (+org.nocturne.page.PageLoader OR +org.nocturne.pool.PagePool)";

        try {
            new ClassMatcher(pattern).match("org.nocturne.pool.PagePool");
            assertFalse(true);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

}
