/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.module;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.main.ApplicationContext;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Scans loaded templates to contains {{...}} and uses captions framework to
 * substitute them to caption values.
 *
 * @author Mike Mirzayanov
 */
public class PreprocessFreemarkerFileTemplateLoader extends MultiTemplateLoader {
    private static final ConcurrentMap<String, InmemoryTemplateSource> templateSourceByName = new ConcurrentHashMap<String, InmemoryTemplateSource>();
    private final int templateDirCount;

    public PreprocessFreemarkerFileTemplateLoader(File... templateDirs) throws IOException {
        super(getTemplateLoaders(templateDirs));
        this.templateDirCount = templateDirs.length;
    }

    private static TemplateLoader[] getTemplateLoaders(File[] templateDirs) throws IOException {
        int templateDirCount = templateDirs.length;
        if (templateDirCount <= 0) {
            throw new ConfigurationException("Please specify at least one template directory.");
        }

        TemplateLoader[] templateLoaders = new TemplateLoader[templateDirCount];

        for (int dirIndex = 0; dirIndex < templateDirCount; ++dirIndex) {
            templateLoaders[dirIndex] = new FileTemplateLoader(templateDirs[dirIndex]);
        }

        return templateLoaders;
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        InmemoryTemplateSource templateSource = templateSourceByName.get(name);
        if (templateSource == null) {
            if (templateDirCount > 1 && !ApplicationContext.getInstance().isStickyTemplatePaths()) {
                resetState();
            }
            return super.findTemplateSource(name);
        } else {
            return templateSource;
        }
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        StringBuilder stringBuilder = getTemplateAsStringBuilder(templateSource, encoding);
        processText(stringBuilder);
        return new StringReader(stringBuilder.toString());
    }

    private StringBuilder getTemplateAsStringBuilder(Object templateSource, String encoding) throws IOException {
        if (templateSource instanceof InmemoryTemplateSource) {
            return new StringBuilder(((InmemoryTemplateSource) templateSource).getContent());
        } else {
            Reader reader = super.getReader(templateSource, encoding);
            StringBuilder builder = new StringBuilder();

            char[] buffer = new char[65536];
            while (true) {
                int readByteCount = reader.read(buffer);
                if (readByteCount == -1) {
                    break;
                }
                builder.append(buffer, 0, readByteCount);
            }
            reader.close();

            return builder;
        }
    }

    /**
     * Scans content to find "{{...some-text...}}" and replaces it using InteropImpl.
     *
     * @param sb content to be processed
     */
    private static void processText(StringBuilder sb) {
        int index = 0;

        while (index + 1 < sb.length()) {
            if (sb.charAt(index) == '{' && sb.charAt(index + 1) == '{') {
                int closeIndex = sb.indexOf("}}", index);

                if (closeIndex >= 0) {
                    String content = sb.substring(index + 2, closeIndex);

                    if (content.startsWith("!")) {
                        throw new UnsupportedOperationException("{{!...}} syntax is no more supported.");
                    }

                    String replacement = ApplicationContext.getInstance().$(content);
                    sb.replace(index, closeIndex + 2, replacement);
                }
            }

            ++index;
        }
    }

    /**
     * Use it to override or setup template source by it's name. Parameter {@code content} will be used
     * as a template source even if ftl-file exists. Current time will be used in cache routine
     * as last modification time of template content.
     *
     * @param name    Template name (for example, "IndexPage.ftl")
     * @param content Template source
     */
    public static void addTemplateSource(String name, String content) {
        templateSourceByName.put(name, new InmemoryTemplateSource(name, content));
    }

    /**
     * Use it to override or setup template source by it's name. Parameter {@code content} will be used
     * as a template source even if ftl-file exists. Parameter {@code modificationTime} will be used in cache routine
     * as last modification time of template content.
     *
     * @param name             Template name (for example, "IndexPage.ftl")
     * @param content          Template source
     * @param modificationTime Last modification time of template content
     */
    public static void addTemplateSource(String name, String content, long modificationTime) {
        templateSourceByName.put(name, new InmemoryTemplateSource(name, content, modificationTime));
    }

    @Override
    public void closeTemplateSource(Object templateSource) throws IOException {
        if (!(templateSource instanceof InmemoryTemplateSource)) {
            super.closeTemplateSource(templateSource);
        }
    }

    @SuppressWarnings("DeserializableClassInSecureContext")
    private static final class InmemoryTemplateSource extends File {
        private final String content;
        private final long modificationTime;

        private InmemoryTemplateSource(String name, String content) {
            this(name, content, System.currentTimeMillis());
        }

        private InmemoryTemplateSource(String name, String content, long modificationTime) {
            super(name);
            this.content = content;
            this.modificationTime = modificationTime;
        }

        private String getContent() {
            return content;
        }

        @SuppressWarnings("RefusedBequest")
        @Override
        public long lastModified() {
            return modificationTime;
        }
    }
}
