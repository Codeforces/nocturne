package org.nocturne.pool;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import org.nocturne.misc.ApplicationContext;

import java.io.File;
import java.io.IOException;

/** @author Mike Mirzayanov */
public class TemplateEngineConfigurationPool extends Pool<Configuration> {
    private final ApplicationContext applicationContext;

    public TemplateEngineConfigurationPool(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected Configuration newInstance() {
        try {
            Configuration templateEngineConfiguration = new Configuration();
            templateEngineConfiguration.setDirectoryForTemplateLoading(
                new File(applicationContext.getTemplatesPath())
            );
            templateEngineConfiguration.setObjectWrapper(new DefaultObjectWrapper());
            return templateEngineConfiguration;
        } catch (IOException e) {
            throw new IllegalStateException("Can't create template engine.", e);
        }
    }
}
