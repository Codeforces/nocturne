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
     * @return Use ";" to separate links. Do not use slash as a first character.
     *         Example: "user/{login};profile".
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
     * Marker interface for type of link.
     */
    interface Type {
    }

    public class Builder {
        public static Link newLink(@Nonnull final String value, @Nonnull final String name, @Nonnull final String action,
                                   @Nonnull final Class<? extends Type>[] types) {
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
            };
        }
    }
}
