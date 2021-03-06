/*
 * Copyright by Mike Mirzayanov
 */
package bloggy.captions.model;

import bloggy.model.Entity;

public class Caption extends Entity {
    private String shortcut;

    private String locale;

    private String value;

    private String shortcutSha1;

    public String getShortcutSha1() {
        return shortcutSha1;
    }

    public String getShortcut() {
        return shortcut;
    }

    public void setShortcut(String shortcut) {
        this.shortcut = shortcut;
    }

    public void setShortcutSha1(String shortcutSha1) {
        this.shortcutSha1 = shortcutSha1;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
