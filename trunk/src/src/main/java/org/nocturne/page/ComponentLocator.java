package org.nocturne.page;

import freemarker.template.Template;

import java.util.HashMap;
import java.util.Map;

/** @author Mike Mirzayanov */
public class ComponentLocator {
    private static ThreadLocal<Map<Template, Component>> maps = new ThreadLocal<Map<Template, Component>>() {
        protected Map<Template, Component> initialValue() {
            return new HashMap<Template, Component>();
        }
    };

    private static ThreadLocal<Page> page = new ThreadLocal<Page>();

    public static void clear() {
        maps.get().clear();
    }

    public static void setPage(Page page) {
        ComponentLocator.page.set(page);
    }

    public static Page getPage() {
        return ComponentLocator.page.get();
    }

    public static Component get(Template template) {
        return maps.get().get(template);
    }

    public static void set(Template template, Component component) {
        maps.get().put(template, component);
    }
}
