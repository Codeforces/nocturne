/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.pool;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import org.apache.log4j.Logger;
import org.nocturne.main.ApplicationTemplateLoader;
import org.nocturne.main.Constants;
import org.nocturne.main.ReloadingContext;

import javax.servlet.FilterConfig;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Storage to store template configurations.
 * Nocturne will not create new configuration on request but reuses old (if exists).
 *
 * @author Mike Mirzayanov
 */
public class TemplateEngineConfigurationPool extends Pool<Configuration> {
    private static final Logger logger = Logger.getLogger(TemplateEngineConfigurationPool.class);

    private final FilterConfig filterConfig;

    private static final AtomicLong count = new AtomicLong(0);
    private volatile TemplateEngineConfigurationHandler handler;

    public TemplateEngineConfigurationPool(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        this.handler = null;
    }

    public void setInstanceHandler(TemplateEngineConfigurationHandler handler) {
        this.handler = handler;
    }

    @Override
    protected Configuration newInstance() {
        Configuration templateEngineConfiguration = new Configuration(Constants.FREEMARKER_VERSION);
        templateEngineConfiguration.setDefaultEncoding(StandardCharsets.UTF_8.name());

        if (!ReloadingContext.getInstance().isDebug()) {
            logger.warn("Processed templateEngineConfiguration.setTemplateUpdateDelay(" + ReloadingContext.getInstance().getTemplatesUpdateDelay() + ").");
            templateEngineConfiguration.setTemplateUpdateDelay(ReloadingContext.getInstance().getTemplatesUpdateDelay());
        }

        templateEngineConfiguration.setTemplateLoader(new ApplicationTemplateLoader());
        templateEngineConfiguration.setObjectWrapper(new DefaultObjectWrapper(Constants.FREEMARKER_VERSION));

        logger.debug("Created instance of Configuration [count=" + count.incrementAndGet() + "].");

        if (handler != null) {
            handler.onInstance(templateEngineConfiguration);
        }
        return templateEngineConfiguration;
    }

    public interface TemplateEngineConfigurationHandler {
        void onInstance(Configuration configuration);
    }
}
