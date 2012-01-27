/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.util;

import java.util.regex.Pattern;

/**
 * Some string utilities. You can use apache StringUtils instead of it.
 *
 * @author Mike Mirzayanov
 */
public class StringUtil {
    /**
     * Dangerous characters for HTML.
     */
    private static final String DANGEROUS_CHARS = "&\"\'<>";

    /**
     * Empty string.
     */
    public static final String EMPTY = "";

    /**
     * @param s String to be checked.
     * @return Is given string empty or null?
     */
    public static boolean isEmptyOrNull(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * @param s String to be checked for HTML-dangerous characters.
     * @return Is given string contain at least one HTML-dangerous character (which should be encoded in HTML)?
     */
    public static boolean containsDangerousCharacters(String s) {
        for (int i = 0; i < s.length(); i++) {
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

    @SuppressWarnings({"HardcodedLineSeparator"})
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
