/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.module;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import org.apache.commons.io.IOUtils;
import org.nocturne.exception.ConfigurationException;
import org.nocturne.main.ApplicationContext;
import org.nocturne.main.ReloadingContext;
import org.nocturne.template.TemplatePreprocessor;
import org.nocturne.template.impl.ComponentTemplatePreprocessor;
import org.nocturne.util.StringUtil;

import javax.security.auth.login.AppConfigurationEntry;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Scans loaded templates to contains {{...}} and uses captions framework to
 * substitute them to caption values. Also prepares @once directive (sets scopes) and
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class PreprocessFreemarkerFileTemplateLoader extends MultiTemplateLoader {
    private static final org.apache.log4j.Logger logger
            = org.apache.log4j.Logger.getLogger(PreprocessFreemarkerFileTemplateLoader.class);

    private static final ConcurrentMap<String, InmemoryTemplateSource> templateSourceByName = new ConcurrentHashMap<>();
    private final int templateDirCount;

    public PreprocessFreemarkerFileTemplateLoader(File... templateDirs) throws IOException {
        super(getTemplateLoaders(templateDirs));
        this.templateDirCount = templateDirs.length;
    }

    private static TemplateLoader[] getTemplateLoaders(File[] templateDirs) throws IOException {
        int templateDirCount = templateDirs.length;
        if (templateDirCount <= 0) {
            logger.error("Please specify at least one template directory.");
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

            Object result = super.findTemplateSource(name);
            if (result != null && !ReloadingContext.getInstance().isDebug()) {
                try (Reader reader = super.getReader(result, StandardCharsets.UTF_8.name())) {
                    addTemplateSource(name, IOUtils.toString(reader));
                }
            }

            return result;
        } else {
            return templateSource;
        }
    }

    @SuppressWarnings("RefusedBequest")
    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        StringBuilder stringBuilder = getTemplateAsStringBuilder(templateSource, encoding);

        if (ApplicationContext.getInstance().isUseComponentTemplates()) {
            TemplatePreprocessor preprocessor = new ComponentTemplatePreprocessor();
            preprocessor.preprocess(templateSource, stringBuilder);
        }

        processCaptions(stringBuilder);
        processOnceDirectiveCalls(templateSource, stringBuilder);
        return new StringReader(stringBuilder.toString());
    }

    private StringBuilder getTemplateAsStringBuilder(Object templateSource, String encoding) throws IOException {
        if (templateSource instanceof InmemoryTemplateSource) {
            return new StringBuilder(((InmemoryTemplateSource) templateSource).getContent());
        } else {
            StringBuilder builder = new StringBuilder();
            try (Reader reader = super.getReader(templateSource, encoding)) {
                char[] buffer = new char[65536];
                while (true) {
                    int readByteCount = reader.read(buffer);
                    if (readByteCount == -1) {
                        break;
                    }
                    builder.append(buffer, 0, readByteCount);
                }
            }

            return builder;
        }
    }

    /**
     * Scans content to find "{{...some-text...}}" and replaces it using InteropImpl.
     *
     * @param sb content to be processed
     */
    private static void processCaptions(StringBuilder sb) {
        int index = 0;

        while (index + 1 < sb.length()) {
            if (sb.charAt(index) == '{' && sb.charAt(index + 1) == '{') {
                int closeIndex = sb.indexOf("}}", index);

                if (closeIndex >= 0) {
                    String content = sb.substring(index + 2, closeIndex);

                    if (content.startsWith("!")) {
                        logger.error("{{!...}} syntax is no more supported.");
                        throw new UnsupportedOperationException("{{!...}} syntax is no more supported.");
                    }

                    String replacement = ApplicationContext.getInstance().$(content);
                    sb.replace(index, closeIndex + 2, replacement);
                }
            }

            index++;
        }
    }

    private void processOnceDirectiveCalls(Object templateSource, StringBuilder sb) {
        int index = 0;
        while (index + 6 < sb.length()) {
            if (sb.charAt(index) == '<' && sb.charAt(index + 1) == '@' && sb.charAt(index + 2) == 'o'
                    && sb.charAt(index + 3) == 'n' && sb.charAt(index + 4) == 'c' && sb.charAt(index + 5) == 'e'
                    && (Character.isWhitespace(sb.charAt(index + 6)) || sb.charAt(index + 6) == '>')) {
                String scopeAttr = " scope=\"" + escape(templateSource.toString()) + ":" + index + "\"";
                sb.insert(index + 6, scopeAttr);
            }
            index++;
        }
    }

    private String escape(String s) {
        if (StringUtil.isEmpty(s)) {
            return s;
        } else {
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) == '\\') {
                    result.append('/');
                    continue;
                }
                if (s.charAt(i) == '\"') {
                    continue;
                }
                result.append(s.charAt(i));
            }
            return result.toString();
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
    public long getLastModified(Object templateSource) {
        if (templateSource == null) {
            return 0;
        }

        if (templateSource instanceof InmemoryTemplateSource) {
            return ((InmemoryTemplateSource) templateSource).lastModified();
        }

        return super.getLastModified(templateSource);
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
