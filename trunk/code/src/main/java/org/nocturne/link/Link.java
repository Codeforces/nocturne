/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.link;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use it to specify link pattern for page.
 *
 * @author Mike Mirzayanov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Link {
    /**
     * @return Use ";" to separate links. Do not use slash as a first character.
     *         Example: "user/{login};profile".
     */
    String value();

    /**
     * @return You can mark link usages with some classes. For example, it
     *         could be menu items or layout types.
     */
    Class<? extends Type>[] types() default {};

    /**
     * Marker interface for type of link.
     */
    public static interface Type {
    }
}