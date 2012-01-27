package org.nocturne.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Set it before page method which should be executed on specified
 * action as validation method. The method should return boolean and
 * the value will define future workflow.
 * <p/>
 * Typically validation methods has the line "return runValidation();" as
 * the last line.
 *
 * @author Mike Mirzayanov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Validate {
    /**
     * @return Action name.
     */
    String value() default "";
}
