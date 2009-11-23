/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * You can specify page name and later easily get links
 * using it.
 *
 * If you don't specify name for page, the
 * page name is equals to page.getClass().getSimpleName().
 *
 * @author Mike Mirzayanov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Name {
    /** @return Page name (you can use this value as page name to find its link). */
    String value();
}
