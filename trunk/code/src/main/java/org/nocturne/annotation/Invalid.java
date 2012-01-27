/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set it before page method which should be executed for specified
 * action if validation fails. Action method will no be executed
 * in this case.
 *
 * @author Mike Mirzayanov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Invalid {
    /**
     * @return Action name.
     */
    String value() default "";
}
