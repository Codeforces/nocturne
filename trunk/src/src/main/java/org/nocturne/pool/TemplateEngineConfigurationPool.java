package org.nocturne.pool;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import org.nocturne.misc.ApplicationContext;

import javax.servlet.FilterConfig;
import java.io.File;
import java.io.IOException;

/** @author Mike Mirzayanov */
public class TemplateEngineConfigurationPool extends Pool<Configuration> {
    private final ApplicationContext applicationContext;
    private final FilterConfig filterConfig;

    public TemplateEngineConfigurationPool(ApplicationContext applicationContext, FilterConfig filterConfig) {
        this.applicationContext = applicationContext;
        this.filterConfig = filterConfig;
    }

    protected Configuration newInstance() {
        try {
            File directory = new File(applicationContext.getTemplatesPath());
            if (!directory.isDirectory()) {
                directory = new File(filterConfig.getServletContext().getRealPath(
                        applicationContext.getTemplatesPath())
                );
            }

            Configuration templateEngineConfiguration = new Configuration();
            templateEngineConfiguration.setDirectoryForTemplateLoading(
                directory
            );
            templateEngineConfiguration.setObjectWrapper(new DefaultObjectWrapper());
            return templateEngineConfiguration;
        } catch (IOException e) {
            throw new IllegalStateException("Can't create template engine.", e);
        }
    }
}
