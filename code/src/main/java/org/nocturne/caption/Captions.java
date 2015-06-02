/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.caption;

import java.util.Locale;

/**
 * <p>
 * Interface through which works ApplicationContext.$() method,
 * "{{shortcut}}" and {@literal <@caption/>} directive in templates.
 * </p>
 * It is native method in nocturne to write internationalized
 * application. Just write in templates "{{some text}}".
 *
 * @author Mike Mirzayanov
 */
public interface Captions {
    /**
     * Returns caption value.
     *
     * @param locale   Locale for which caption value to find.
     * @param shortcut Caption shortcut.
     * @param args     of type Arguments (if value has placeholders like "Hello, {0}").
     * @return String Caption value.
     */
    String find(Locale locale, String shortcut, Object... args);


    /**
     * Returns caption value. Uses current application locale (see ApplicationContext.getInstance().getLocale()).
     * Typical implementation is
     * <pre>
     *     public String find(String shortcut, Object... args) {
     *         return find(ApplicationContext.getInstance().getLocale(), shortcut, args);
     *     }
     * </pre>
     *
     * @param shortcut Caption shortcut.
     * @param args     of type Arguments (if value has placeholders like "Hello, {0}").
     * @return String Caption value.
     */
    String find(String shortcut, Object... args);
}
