/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.annotation;

import static java.lang.annotation.ElementType.FIELD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * If your component (page or frame) has field marked with
 * Parameter annotation then nocturne will set it automatically before
 * beforeAction phase.
 * <p/>
 * Strip mode controls valid values for parameter.
 * <p/>
 * Name means name of the parameter
 * as GET or POST parameter or part
 * of overrideParameters in the response of RequestRouter (LinkedRequestRouter places
 * {param} shortcuts in it).
 *
 * @author Mike Mirzayanov
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Parameter {
    /** @return POST or GET parameter name. Don't set it if it is the same with class field name. */
    String name() default "";

    /** @return What strategy to strip characters to use. Default value is the most strict StripMode.ID. */
    StripMode stripMode() default StripMode.ID;

    /** How to strip parameter value. */
    enum StripMode {
        /** Do not strip any characters. */
        NONE {
            /** @see org.nocturne.annotation.Parameter.StripMode#strip(String) */
            public String strip(String value) {
                return value;
            }
        },

        /** Leave only safe chars: strip slashes, quotes and low-code chars. Also makes trim(). */
        SAFE {
            /** @see org.nocturne.annotation.Parameter.StripMode#strip(String) */
            public String strip(String value) {
                if (value != null) {
                    char[] chars = value.toCharArray();
                    StringBuilder sb = new StringBuilder(chars.length);
                    for (char c : chars) {
                        if (c != '/' && c != '\\' && c != '\"' && c != '\'' && c >= ' ') {
                            sb.append(c);
                        }
                    }
                    return sb.toString().trim();
                } else {
                    return value;
                }
            }
        },

        /** Leave only chars which can be part of java ID (see Character.isJavaIdentifierPart). */
        ID {
            private boolean isValidChar(char c) {
                return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')
                        || ('0' <= c && c <= '9') || (c == '_')
                        || (c == '-')
                        || (c == '.');
            }

            /** @see org.nocturne.annotation.Parameter.StripMode#strip(String) */
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
