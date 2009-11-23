/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.pool;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import org.nocturne.exception.FreemarkerException;
import org.nocturne.main.ApplicationContext;
import org.nocturne.main.ApplicationTemplateLoader;

import javax.servlet.FilterConfig;
import java.io.File;
import java.io.IOException;

/**
 * Storage to store template configurations.
 * Nocturne will not create new configuration on request but reuses old (if exists).
 *
 * @author Mike Mirzayanov
 */
public class TemplateEngineConfigurationPool extends Pool<Configuration> {
    private final FilterConfig filterConfig;

    public TemplateEngineConfigurationPool(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    protected Configuration newInstance() {
        try {
            File directory = new File(ApplicationContext.getInstance().getTemplatesPath());

            if (!directory.isDirectory()) {
                directory = new File(filterConfig.getServletContext().getRealPath(
                        ApplicationContext.getInstance().getTemplatesPath())
                );
            }

            Configuration templateEngineConfiguration = new Configuration();
            templateEngineConfiguration.setDirectoryForTemplateLoading(
                    directory
            );
            templateEngineConfiguration.setDefaultEncoding("UTF-8");

            setupTemplateLoaderClass(templateEngineConfiguration);
            templateEngineConfiguration.setObjectWrapper(new DefaultObjectWrapper());
            return templateEngineConfiguration;
        } catch (IOException e) {
            throw new FreemarkerException("Can't create template engine.", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void setupTemplateLoaderClass(Configuration templateEngineConfiguration) {
        templateEngineConfiguration.setTemplateLoader(new ApplicationTemplateLoader());
    }
}
