/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.module;

import freemarker.cache.FileTemplateLoader;
import org.nocturne.main.ApplicationContext;

import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;

/**
 * Scans loaded templates to contains {{...}} and uses captions framework to
 * substitutes them to caption values.
 *
 * @author Mike Mirzayanov
 */
public class PreprocessFreemarkerFileTemplateLoader extends FileTemplateLoader {
    public PreprocessFreemarkerFileTemplateLoader(File baseDir) throws IOException {
        super(baseDir);
    }

    @Override
    public Object findTemplateSource(String name) throws IOException {
        return super.findTemplateSource(name);
    }

    @Override
    public Reader getReader(Object templateSource, String encoding) throws IOException {
        Reader reader = super.getReader(templateSource, encoding);
        StringBuffer sb = new StringBuffer();

        char[] buffer = new char[65536];
        while (true) {
            int readByteCount = reader.read(buffer);
            if (readByteCount == -1) {
                break;
            }
            sb.append(buffer, 0, readByteCount);
        }

        reader.close();
        processText(sb);

        char[] chars = sb.toString().toCharArray();
        return new CharArrayReader(chars);
    }

    /**
     * Scans content to find "{{...some-text...}}" and replace it using InteropImpl.
     *
     * @param sb Content to be processes.
     */
    private static void processText(StringBuffer sb) {
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
}
