/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import freemarker.cache.TemplateLoader;
import org.apache.log4j.Logger;
import org.nocturne.exception.NocturneException;
import org.nocturne.module.Module;
import org.nocturne.module.PreprocessFreemarkerFileTemplateLoader;
import org.nocturne.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * This template loader will delegate all the requests to
 * standard FileTemplateLoader in production mode or
 * loads template from modules DebugContext in debug mode.
 */
public class ApplicationTemplateLoader implements TemplateLoader {
    /**
     * Logger.
     */
    private static final Logger logger = Logger.getLogger(ApplicationTemplateLoader.class);

    /**
     * List of loaded modules.
     */
    private final List<Module> modules;

    /**
     * Instance of ApplicationContext - just shortcut.
     */
    private final ApplicationContext applicationContext = ApplicationContext.getInstance();

    /**
     * For debug mode stores loader by loaded object.
     */
    private final Map<Object, TemplateLoader> loadersByTemplate = new WeakHashMap<>();

    /**
     * Usual file template loader, uses nocturne.templates-path.
     */
    private final TemplateLoader templateLoader;

    /**
     * New ApplicationTemplateLoader.
     */
    public ApplicationTemplateLoader() {
        modules = applicationContext.getModules();

        String[] templatePaths = applicationContext.getTemplatePaths();
        int templateDirCount = templatePaths.length;
        File[] templateDirs = new File[templateDirCount];

        for (int dirIndex = 0; dirIndex < templateDirCount; ++dirIndex) {
            String templatePath = templatePaths[dirIndex];
            File templatePathFile = new File(templatePath);

            if (!templatePathFile.isAbsolute() || !templatePathFile.exists()) {
                String realTemplatePath = FileUtil.getRealPath(applicationContext.getServletContext(), templatePath);
                if (realTemplatePath == null) {
                    throw new NocturneException("Can't find '" + templatePath + "' in servletContext.");
                } else {
                    templatePath = realTemplatePath;
                }
            }

            templatePathFile = new File(templatePath);
            if (!templatePathFile.exists()) {
                throw new NocturneException("Can't find template path '" + templatePath + "' in servletContext.");
            }

            templateDirs[dirIndex] = templatePathFile;
        }

        try {
            templateLoader = new PreprocessFreemarkerFileTemplateLoader(templateDirs);
        } catch (IOException e) {
            throw new NocturneException("Can't create FileTemplateLoader for delegation.", e);
        }
    }

    @Override
    public Object findTemplateSource(String s) throws IOException {
        if (applicationContext.isDebug()) {
            for (Module module : modules) {
                Object result = module.getTemplateLoader().findTemplateSource(s);
                if (result != null) {
                    loadersByTemplate.put(result, module.getTemplateLoader());
                    return result;
                }
            }
        }
        return templateLoader.findTemplateSource(s);
    }

    @Override
    public long getLastModified(Object o) {
        if (applicationContext.isDebug() && loadersByTemplate.containsKey(o)) {
            return loadersByTemplate.get(o).getLastModified(o);
        }
        return templateLoader.getLastModified(o);
    }

    @Override
    public Reader getReader(Object o, String s) throws IOException {
        if (applicationContext.isDebug() && loadersByTemplate.containsKey(o)) {
            return loadersByTemplate.get(o).getReader(o, s);
        }

        return templateLoader.getReader(o, s);
    }

    @Override
    public void closeTemplateSource(Object o) throws IOException {
        if (applicationContext.isDebug() && loadersByTemplate.containsKey(o)) {
            TemplateLoader loader = loadersByTemplate.get(o);
            if (loader != null) {
                loader.closeTemplateSource(o);
                loadersByTemplate.remove(o);
            }
        }

        templateLoader.closeTemplateSource(o);
    }
}
