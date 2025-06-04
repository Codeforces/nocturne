/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.annotation;

import org.nocturne.main.HttpMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Set it before page method which should be executed on specified
 * action. Action is a string parameter with name "action".
 * </p>
 * <p>
 * If you have set @Action without value, it means that default action
 * became the annotated method but not action().
 * </p>
 * <p>
 * If there is validation method (see @Validate.class) action method
 * will be invoked only on if validation passed.
 * </p>
 *
 * @author Mike Mirzayanov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
    /**
     * @return Action name.
     */
    String value() default "";

    /**
     * @return HTTP method to be handled by the action.
     */
    HttpMethod[] method() default {HttpMethod.GET};
}
