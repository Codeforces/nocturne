/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * If your component (page or frame) has fields marked with
 * a @Parameter then nocturne will set them automatically before
 * beforeAction phase. Also nocturne takes care about parameters of
 * action/validate/invalid Controller method parameters.
 * </p>
 * <p>
 * Strip mode controls valid values for parameter.
 * </p>
 * <p>
 * Name means name of the parameter
 * as GET or POST parameter or part
 * of overrideParameters in the response of RequestRouter (LinkedRequestRouter places
 * {param} shortcuts in it).
 * </p>
 *
 * @author Mike Mirzayanov
 */
@Retention(RUNTIME)
@Target({FIELD, PARAMETER})
public @interface Parameter {
    /**
     * @return POST or GET parameter name. Don't set it if it is the same with class field name.
     */
    String name() default "";

    /**
     * @return What strategy to strip characters to use. Default value is the most strict StripMode.ID.
     */
    StripMode stripMode() default StripMode.ID;

    /**
     * How to strip parameter value.
     */
    enum StripMode {
        /**
         * Do not strip any characters.
         */
        NONE {
            @Override
            public String strip(String value) {
                return value;
            }
        },

        /**
         * Leave only safe chars: strip slashes, quotes, angle brackets, ampersand and low-code chars. Also makes trim().
         */
        SAFE {
            @Override
            public String strip(String value) {
                if (value != null) {
                    char[] chars = value.toCharArray();
                    StringBuilder sb = new StringBuilder(chars.length);
                    for (char c : chars) {
                        if (c != '/' && c != '&' && c != '<' && c != '>' && c != '\\' && c != '\"' && c != '\'' && c >= ' ') {
                            sb.append(c);
                        }
                    }
                    return sb.toString().trim();
                } else {
                    return value;
                }
            }
        },

        /**
         * Leave only chars which can be part of java ID (see Character.isJavaIdentifierPart).
         */
        ID {
            private boolean isValidChar(char c) {
                return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                        || (c >= '0' && c <= '9') || (c == '_')
                        || (c == '-')
                        || (c == '.');
            }

            @Override
            public String strip(String value) {
                if (value != null) {
                    char[] chars = value.toCharArray();
                    StringBuilder sb = new StringBuilder(chars.length);
                    for (char c : chars) {
                        if (isValidChar(c)) {
                            sb.append(c);
                        }
                    }
                    return sb.toString();
                } else {
                    return value;
                }
            }
        };

        /**
         * @param value Value to be stripped.
         * @return Processed value.
         */
        public abstract String strip(String value);
    }
}
