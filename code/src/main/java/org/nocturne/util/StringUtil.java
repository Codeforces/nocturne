/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.util;

/**
 * Some string utilities. You can use apache StringUtils instead of it.
 *
 * @author Mike Mirzayanov
 */
public class StringUtil {
    /** Dangerous characters for HTML. */
    private static String DANGEROUS_CHARS = "&\"\'<>";

    /** Empty string. */
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
}