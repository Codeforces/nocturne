package org.nocturne.main;

/**
 * @author Mike Mirzayanov
 */
public class ApplicationContextHelper {
    public static void setContextPath(String contextPath) {
        ApplicationContext.getInstance().setContextPath(contextPath);
    }
}
