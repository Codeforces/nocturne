/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set it before page method which should be executed on specified
 * action. Action is a string parameter with name "action".
 * <p/>
 * If you have set @Action without value, it means that default action
 * bacame the annotated method but not action().
 * <p/>
 * If there is validation method (see @Validate.class) action method
 * will be invoked only on if validation passed.
 *
 * @author Mike Mirzayanov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
    /** @return Action name. */
    String value() default "";
}
