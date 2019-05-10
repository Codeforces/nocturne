package bloggy;

import com.codeforces.commons.properties.PropertiesUtil;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Mike Mirzayanov
 */
public class ApplicationServletContextListener implements ServletContextListener {
    private static final AtomicBoolean destroyed = new AtomicBoolean();

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("Context has been initialized.");

        String timezone = PropertiesUtil.getProperty("timezone",
                "Europe/Moscow", "/application.properties");
        TimeZone.setDefault(TimeZone.getTimeZone(timezone));
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("Context has been destroyed.");
        destroyed.set(true);
    }

    public static boolean getDestroyed() {
        return destroyed.get();
    }
}
