/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.link;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Mike Mirzayanov
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface LinkSet {
    Link[] value() default {};
}
