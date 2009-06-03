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

    private static ThreadLocal<Page> currentPage = new ThreadLocal<Page>();
    private static ThreadLocal<Component> currentComponent = new ThreadLocal<Component>();

    public static void clear() {
        maps.get().clear();
    }

    @Deprecated
    public static void setPage(Page page) {
        setCurrentPage(page);
    }

    @Deprecated
    public static Page getPage() {
        return getCurrentPage();
    }

    /**
     * Do not call this method. It will be invoked
     * automatically by nocturne.
     *
     * @param page Current rendering page.
     */
    public static void setCurrentPage(Page page) {
        ComponentLocator.currentPage.set(page);
    }

    /** @return Current rendering page instance. */
    public static Page getCurrentPage() {
        return ComponentLocator.currentPage.get();
    }

    /**
     * Do not call this method. It will be invoked
     * automatically by nocturne.
     *
     * @param component Current rendering component.
     */
    public static void setCurrentComponent(Component component) {
        ComponentLocator.currentComponent.set(component);
    }

    /** @return Current rendering frame or page. */
    public static Component getCurrentComponent() {
        return currentComponent.get();
    }

    public static Component get(Template template) {
        return maps.get().get(template);
    }

    public static void set(Template template, Component component) {
        maps.get().put(template, component);
    }
}
