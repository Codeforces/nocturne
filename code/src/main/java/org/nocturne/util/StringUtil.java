/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.regex.Pattern;

/**
 * Some string utilities. You can use apache StringUtils instead of it.
 *
 * @author Mike Mirzayanov
 * @author Maxim Shipko (sladethe@gmail.com)
 */
public class StringUtil {
    /**
     * Dangerous characters for HTML.
     */
    private static final String DANGEROUS_CHARS = "&\"\'<>";

    static final char NON_BREAKING_SPACE = (char) 160;
    static final char THIN_SPACE = '\u2009';
    static final char ZERO_WIDTH_SPACE = '\u200B';

    public static boolean isWhitespace(char c) {
        return Character.isWhitespace(c) || c == NON_BREAKING_SPACE || c == ZERO_WIDTH_SPACE;
    }

    /**
     * @param s String.
     * @return {@code true} iff {@code s} is {@code null} or empty.
     */
    public static boolean isEmpty(@Nullable String s) {
        return s == null || s.isEmpty();
    }

    /**
     * @param s String.
     * @return {@code true} iff {@code s} is not {@code null} and not empty.
     */
    public static boolean isNotEmpty(@Nullable String s) {
        return s != null && !s.isEmpty();
    }

    /**
     * @param s String.
     * @return {@code true} iff {@code s} is {@code null}, empty or contains only whitespaces.
     * @see #isWhitespace(char)
     */
    public static boolean isBlank(@Nullable String s) {
        if (s == null || s.isEmpty()) {
            return true;
        }

        for (int charIndex = s.length() - 1; charIndex >= 0; --charIndex) {
            if (!isWhitespace(s.charAt(charIndex))) {
                return false;
            }
        }

        return true;
    }

    /**
     * @param s String.
     * @return {@code true} iff {@code s} is not {@code null}, not empty
     *         and contains at least one character that is not whitespace.
     * @see #isWhitespace(char)
     */
    public static boolean isNotBlank(@Nullable String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        for (int charIndex = s.length() - 1; charIndex >= 0; --charIndex) {
            if (!isWhitespace(s.charAt(charIndex))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Compares two strings case-sensitive.
     *
     * @param stringA first string
     * @param stringB second string
     * @return {@code true} iff both strings A and B are {@code null}
     *         or the string B represents a {@code String} equivalent to the string A
     */
    public static boolean equals(@Nullable String stringA, @Nullable String stringB) {
        return stringA == null ? stringB == null : stringA.equals(stringB);
    }

    /**
     * Compares two strings case-sensitive. {@code null} and empty values considered equals.
     *
     * @param stringA first string
     * @param stringB second string
     * @return {@code true} iff both strings A and B are {@link #isEmpty(String) empty}
     *         or the string B represents a {@code String} equal to the string A
     */
    public static boolean equalsOrEmpty(@Nullable String stringA, @Nullable String stringB) {
        return isEmpty(stringA) ? isEmpty(stringB) : stringA.equals(stringB);
    }

    /**
     * Compares two strings case-sensitive. {@code null}, empty and blank values considered equals.
     *
     * @param stringA first string
     * @param stringB second string
     * @return {@code true} iff both strings A and B are {@link #isBlank(String) blank}
     *         or the string B represents a {@code String} equal to the string A
     */
    public static boolean equalsOrBlank(@Nullable String stringA, @Nullable String stringB) {
        return isBlank(stringA) ? isBlank(stringB) : stringA.equals(stringB);
    }

    /**
     * Compares two strings case-insensitive.
     *
     * @param stringA first string
     * @param stringB second string
     * @return {@code true} iff both strings A and B are {@code null}
     *         or the string B represents a {@code String} equal to the string A
     */
    public static boolean equalsIgnoreCase(@Nullable String stringA, @Nullable String stringB) {
        return stringA == null ? stringB == null : stringA.equalsIgnoreCase(stringB);
    }

    /**
     * Compares two strings case-insensitive. {@code null} and empty values considered equals.
     *
     * @param stringA first string
     * @param stringB second string
     * @return {@code true} iff both strings A and B are {@link #isEmpty(String) empty}
     *         or the string B represents a {@code String} equal to the string A
     */
    public static boolean equalsOrEmptyIgnoreCase(@Nullable String stringA, @Nullable String stringB) {
        return isEmpty(stringA) ? isEmpty(stringB) : stringA.equalsIgnoreCase(stringB);
    }

    /**
     * Compares two strings case-insensitive. {@code null}, empty and blank values considered equals.
     *
     * @param stringA first string
     * @param stringB second string
     * @return {@code true} iff both strings A and B are {@link #isBlank(String) blank}
     *         or the string B represents a {@code String} equal to the string A
     */
    public static boolean equalsOrBlankIgnoreCase(@Nullable String stringA, @Nullable String stringB) {
        return isBlank(stringA) ? isBlank(stringB) : stringA.equalsIgnoreCase(stringB);
    }

    public static int length(@Nullable String s) {
        return s == null ? 0 : s.length();
    }

    @Nonnull
    public static String nullToEmpty(@Nullable String s) {
        return s == null ? "" : s;
    }

    @Nullable
    public static String emptyToNull(@Nullable String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    @Nullable
    public static String trim(@Nullable String s) {
        if (s == null) {
            return null;
        }

        int lastIndex = s.length() - 1;
        int beginIndex = 0;
        int endIndex = lastIndex;

        while (beginIndex <= lastIndex && isWhitespace(s.charAt(beginIndex))) {
            ++beginIndex;
        }

        while (endIndex > beginIndex && isWhitespace(s.charAt(endIndex))) {
            --endIndex;
        }

        return beginIndex == 0 && endIndex == lastIndex ? s : s.substring(beginIndex, endIndex + 1);
    }

    @Nullable
    public static String trimToNull(@Nullable String s) {
        return s == null ? null : (s = trim(s)).isEmpty() ? null : s;
    }

    @Nonnull
    public static String trimToEmpty(@Nullable String s) {
        return s == null ? "" : trim(s);
    }

    @Nullable
    public static String trimRight(@Nullable String s) {
        if (s == null) {
            return null;
        }

        int lastIndex = s.length() - 1;
        int endIndex = lastIndex;

        while (endIndex >= 0 && isWhitespace(s.charAt(endIndex))) {
            --endIndex;
        }

        return endIndex == lastIndex ? s : s.substring(0, endIndex + 1);
    }

    @Nullable
    public static String trimLeft(@Nullable String s) {
        if (s == null) {
            return null;
        }

        int lastIndex = s.length() - 1;
        int beginIndex = 0;

        while (beginIndex <= lastIndex && isWhitespace(s.charAt(beginIndex))) {
            ++beginIndex;
        }

        return beginIndex == 0 ? s : s.substring(beginIndex, lastIndex + 1);
    }

    /**
     * @param s String to be checked for HTML-dangerous characters.
     * @return Is given string contain at least one HTML-dangerous character (which should be encoded in HTML)?
     */
    public static boolean containsDangerousCharacters(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (DANGEROUS_CHARS.indexOf(s.charAt(i)) >= 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param c Character to be checked.
     * @return Is character should be encoded for HTNL?
     */
    public static boolean isDangerousCharacter(char c) {
        return DANGEROUS_CHARS.indexOf(c) >= 0;
    }

    public static class Patterns {
        private Patterns() {
            throw new UnsupportedOperationException();
        }

        public static final Pattern PLUS_PATTERN = Pattern.compile("\\+");
        public static final Pattern MINUS_PATTERN = Pattern.compile("\\-");
        public static final Pattern EQ_PATTERN = Pattern.compile("=");
        public static final Pattern LT_PATTERN = Pattern.compile("<");
        public static final Pattern GT_PATTERN = Pattern.compile(">");
        public static final Pattern SPACE_PATTERN = Pattern.compile(" ");
        public static final Pattern NBSP_PATTERN = Pattern.compile("" + (char) 160);
        public static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
        public static final Pattern THIN_SPACE_PATTERN = Pattern.compile("" + '\u2009');
        public static final Pattern ZERO_WIDTH_SPACE_PATTERN = Pattern.compile("" + '\u200B');
        public static final Pattern TAB_PATTERN = Pattern.compile("\t");
        public static final Pattern CR_LF_PATTERN = Pattern.compile("\r\n");
        public static final Pattern CR_PATTERN = Pattern.compile("\r");
        public static final Pattern LF_PATTERN = Pattern.compile("\n");
        public static final Pattern SLASH_PATTERN = Pattern.compile("/");
        public static final Pattern COMMA_PATTERN = Pattern.compile(",");
        public static final Pattern SEMICOLON_PATTERN = Pattern.compile(";");
        public static final Pattern COLON_PATTERN = Pattern.compile(":");
        public static final Pattern AMP_PATTERN = Pattern.compile("&");
    }
}
