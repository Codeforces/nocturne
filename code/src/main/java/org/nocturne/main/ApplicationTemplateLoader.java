/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.TemplateLoader;
import org.nocturne.exception.NocturneException;
import org.nocturne.module.Module;
import org.nocturne.module.PreprocessFreemarkerFileTemplateLoader;

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
    /** List of loaded modules. */
    private List<Module> modules;

    /** Instance of ApplicationContext - just shortcut. */
    private ApplicationContext applicationContext = ApplicationContext.getInstance();

    /** For debug mode stores loader by loaded object. */
    private Map<Object, TemplateLoader> loadersByTemplate = new WeakHashMap<Object, TemplateLoader>();

    /** Usual file template loader, uses nocturne.templates-path. */
    private FileTemplateLoader fileTemplateLoader;

    /** New ApplicationTemplateLoader. */
    public ApplicationTemplateLoader() {
        modules = applicationContext.getModules();

        String templateDir = applicationContext.getTemplatesPath();

        if (!new File(templateDir).isAbsolute()) {
            templateDir = applicationContext.getServletContext().getRealPath(templateDir);
        }

        try {
            fileTemplateLoader = new PreprocessFreemarkerFileTemplateLoader(new File(templateDir));
        } catch (IOException e) {
            throw new NocturneException("Can't create FileTemplateLoader for delegation.", e);
        }
    }

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
        return fileTemplateLoader.findTemplateSource(s);
    }

    public long getLastModified(Object o) {
        if (applicationContext.isDebug() && loadersByTemplate.containsKey(o)) {
            return loadersByTemplate.get(o).getLastModified(o);
        }
        return fileTemplateLoader.getLastModified(o);
    }

    public Reader getReader(Object o, String s) throws IOException {
        if (applicationContext.isDebug() && loadersByTemplate.containsKey(o)) {
            return loadersByTemplate.get(o).getReader(o, s);
        }
        return fileTemplateLoader.getReader(o, s);

    }

    public void closeTemplateSource(Object o) throws IOException {
        if (applicationContext.isDebug() && loadersByTemplate.containsKey(o)) {
            TemplateLoader loader = loadersByTemplate.get(o);
            if (loader != null) {
                loader.closeTemplateSource(o);
                loadersByTemplate.remove(o);
            }
        }
        fileTemplateLoader.closeTemplateSource(o);
    }
}
