/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.pool;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import org.apache.log4j.Logger;
import org.nocturne.main.ApplicationTemplateLoader;
import org.nocturne.main.ReloadingContext;

import javax.servlet.FilterConfig;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Storage to store template configurations.
 * Nocturne will not create new configuration on request but reuses old (if exists).
 *
 * @author Mike Mirzayanov
 */
public class TemplateEngineConfigurationPool extends Pool<Configuration> {
    private static final Logger logger = Logger.getLogger(TemplateEngineConfigurationPool.class);
    private static final int DEBUG_TEMPLATE_UPDATE_TIME_SECONDS = 60;

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
        Configuration templateEngineConfiguration = new Configuration();
        templateEngineConfiguration.setDefaultEncoding("UTF-8");

        if (!ReloadingContext.getInstance().isDebug()) {
            templateEngineConfiguration.setTemplateUpdateDelay(DEBUG_TEMPLATE_UPDATE_TIME_SECONDS);
        }

        templateEngineConfiguration.setTemplateLoader(new ApplicationTemplateLoader());
        templateEngineConfiguration.setObjectWrapper(new DefaultObjectWrapper());

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
