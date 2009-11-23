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
}
