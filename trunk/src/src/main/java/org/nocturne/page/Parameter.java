/* Copyright by Mike Mirzayanov. */

package org.nocturne.page;

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * If your component (page or frame) has field marked with
 * Parameter annotation then nocturne will set it automatically on
 * beforeRender phase.
 *
 * @author Mike Mirzayanov
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Parameter {
    /** @return POST or GET parameter name. Don't set it if it is the same with class field name. */
    String name() default "";

    /** @return What strategy to strip characters to use. */
    StripMode stripMode() default StripMode.ID;

    enum StripMode {
        /** Do not strip any characters. */
        NONE {
            String strip(String value) {
                return value;
            }
        },

        /** Leave only safe chars: strip slashes, quotes and low-code chars. Also makes trim(). */
        SAFE {
            String strip(String value) {
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
            String strip(String value) {
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

        abstract String strip(String value);
    }
}
