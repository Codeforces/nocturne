package org.nocturne.pool;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.cache.TemplateLoader;
import freemarker.cache.FileTemplateLoader;
import org.nocturne.misc.ApplicationContext;

import javax.servlet.FilterConfig;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/** @author Mike Mirzayanov */
public class TemplateEngineConfigurationPool extends Pool<Configuration> {
    private static final String TEMPLATE_LOADER_CLASS_PARAMETER_KEY = "nocturne.template-loader-class";

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

            setupTemplateLoaderClass(templateEngineConfiguration, directory);
            templateEngineConfiguration.setObjectWrapper(new DefaultObjectWrapper());
            return templateEngineConfiguration;
        } catch (IOException e) {
            throw new IllegalStateException("Can't create template engine.", e);
        }
    }

    @SuppressWarnings({"unchecked"})
    private void setupTemplateLoaderClass(Configuration templateEngineConfiguration, File templateDir) {
        String templateLoaderClassParam = filterConfig.getInitParameter(TEMPLATE_LOADER_CLASS_PARAMETER_KEY);

        if (templateLoaderClassParam != null) {
            try {
                Class<? extends TemplateLoader> templateLoaderClass =
                        (Class<? extends TemplateLoader>) TemplateEngineConfigurationPool.class.getClassLoader().loadClass(templateLoaderClassParam);

                TemplateLoader loader;
                if (FileTemplateLoader.class.isAssignableFrom(templateLoaderClass)) {
                    try {
                        Constructor<? extends TemplateLoader> fileConstructor = templateLoaderClass.getConstructor(File.class);
                        loader = fileConstructor.newInstance(templateDir);
                    } catch (NoSuchMethodException e) {
                        throw new IllegalStateException("Expected constructor from File for FileTemplateLoader " +
                                "and derived classes [class=" + templateLoaderClass.getName() + "].", e);
                    } catch (InvocationTargetException e) {
                        throw new IllegalStateException("Can't invoke constructor from File for FileTemplateLoader " +
                                "or derived class [class=" + templateLoaderClass.getName() + "].", e);
                    }
                } else {
                    loader = templateLoaderClass.newInstance();
                }
                templateEngineConfiguration.setTemplateLoader(loader);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Can't find template loader class "
                        + templateLoaderClassParam + ".", e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Not enough access to instantiate template loader class "
                        + templateLoaderClassParam + ".", e);
            } catch (InstantiationException e) {
                throw new IllegalStateException("Can't instantiate template loader class "
                        + templateLoaderClassParam + ".", e);
            }
        }
    }
}
