package org.nocturne.page;

import java.util.Map;

/** @author Mike Mirzayanov */
public interface PageClassNameResolver {
    /**
     * Override this method to return page class name.
     * Can be {@code
     * <pre>
     *     return "example.webmail.pages." + pageName;
     * </pre>
     * }
     *
     * @param path Page path, for example "/login";
     * @param parameterMap Contains parameters.
     * @return String Page class name.
     */
    String getPageClassName(String path, Map<String, String> parameterMap);
}
