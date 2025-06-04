/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.link;

import com.google.common.base.Strings;

import javax.annotation.Nonnull;
import java.lang.annotation.*;

/**
 * Use it to specify link pattern for page.
 *
 * @author Mike Mirzayanov
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Link {
    /**
     * @return <p>
     *         Use ";" to separate links. Do not use slash as a first character.
     *         </p>
     *         <p>
     *         Example:
     *         - "user/{login};profile" - first link contains 'login' parameter, second link contains no parameters;
     *         - "user/{login(!blank,alphanumeric):!admin}" - link contains 'login' parameter with restrictions
     *         (should not be blank and should contain only letters and digits, also should not be equal to 'admin');
     *         - "book/{bookId(long,positive)}" - link contains 'bookId' parameter with restrictions (should be a
     *         positive long integer);
     *         - "action/{action:purchase,sell,!action}" - link contains 'action' parameter with restrictions (should
     *         be either equal to 'purchase' or 'sell' and should not be equal to 'action').
     *         </p>
     *         <p>
     *         List of restrictions: null, empty, blank, alpha, numeric, alphanumeric, byte, short, int, long, float,
     *         double, positive, nonpositive, negative, nonnegative, zero, nonzero.
     *         </p>
     */
    String value();

    /**
     * @return Link name: use it together with getRequest().getAttribute("nocturne.current-page-link").
     */
    String name() default "";

    /**
     * @return Default action for page, it will be invoked if no action specified as request parameter.
     */
    String action() default "";

    /**
     * @return You can mark link usages with some classes. For example, it
     *         could be menu items or layout types.
     */
    Class<? extends Type>[] types() default {};

    /**
     * @return List of interceptors to skip.
     */
    String[] skipInterceptors() default {};

    /**
     * Marker interface for type of link.
     */
    interface Type {
    }

    class Builder {
        public static Link newLink(@Nonnull String value, @Nonnull String name,
                                   @Nonnull String action, @Nonnull Class<? extends Type>[] types) {
            return new Link() {
                @Override
                public String value() {
                    return value;
                }

                @Override
                public String name() {
                    return Strings.nullToEmpty(name);
                }

                @Override
                public String action() {
                    return Strings.nullToEmpty(action);
                }

                @Override
                public Class<? extends Type>[] types() {
                    return types;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return Link.class;
                }

                @Override
                public String[] skipInterceptors() {
                    return new String[0];
                }
            };
        }
    }
}
