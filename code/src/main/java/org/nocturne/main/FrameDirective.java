/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import freemarker.core.Environment;
import freemarker.template.*;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.nocturne.template.impl.ComponentTemplatePreprocessor.UNIQUE_MAGIC_OPEN_PREFIX;
import static org.nocturne.template.impl.ComponentTemplatePreprocessor.UNIQUE_MAGIC_CLOSE_PREFIX;

/**
 * Use code like {@code <@frame name="loginFormFrame"/>} to inject
 * frame content into some template. But frame should be parsed into
 * variable loginFormFrame on action processing phase.
 *
 * @author Mike Mirzayanov
 */
public class FrameDirective implements TemplateDirectiveModel {
    private final Set<String> UNIQUE_RENDER_KEYS = new HashSet<>();

    FrameDirective() {
        // No operations.
    }

    @Override
    public void execute(Environment environment, Map map, TemplateModel[] templateModels, TemplateDirectiveBody templateDirectiveBody) throws TemplateException, IOException {
        if (map.size() != 1) {
            throw new TemplateException("Frame directive expects the only 'name' argument.", environment);
        }

        if (!map.containsKey("name")) {
            throw new TemplateException("Frame directive expects the only 'name' argument.", environment);
        }

        Object name = map.get("name");

        if (name instanceof SimpleScalar) {
            Component component = ApplicationContext.getInstance().getCurrentComponent();

            String frameName = ((TemplateScalarModel) name).getAsString();
            String html = component.getFrameHtml(frameName);
            if (html == null) {
                throw new TemplateException("Frame directive expected parsed frame '" + frameName + "', but didn't find.", environment);
            } else {
                if (html.contains(UNIQUE_MAGIC_OPEN_PREFIX)) {
                    html = processComponentUniques(new StringBuilder(html));
                }
            }

            environment.getOut().write(html);
            environment.getOut().flush();
        } else {
            throw new TemplateException("Frame directive parameter 'name' should be a String.", environment);
        }
    }

    @Nonnull
    String processComponentUniques(@Nonnull StringBuilder sb) {
        int open;
        while ((open = sb.indexOf(UNIQUE_MAGIC_OPEN_PREFIX)) >= 0) {
            int close = sb.indexOf(UNIQUE_MAGIC_CLOSE_PREFIX, open + 1);
            if (open < close) {
                int i = open + UNIQUE_MAGIC_OPEN_PREFIX.length();
                while (i < sb.length() && sb.charAt(i) != '>') {
                    i++;
                }
                int j = close + UNIQUE_MAGIC_CLOSE_PREFIX.length();
                while (j < sb.length() && sb.charAt(j) != '>') {
                    j++;
                }
                if (i < sb.length() && sb.charAt(i) == '>'
                        && j < sb.length() && sb.charAt(j) == '>') {
                    String openKey = sb.substring(open + UNIQUE_MAGIC_OPEN_PREFIX.length(), i);
                    String closeKey = sb.substring(close + UNIQUE_MAGIC_CLOSE_PREFIX.length(), j);
                    if (openKey.equals(closeKey) && !openKey.isEmpty()) {
                        if (UNIQUE_RENDER_KEYS.contains(openKey)) {
                            sb.delete(open, j + 2);
                        } else {
                            UNIQUE_RENDER_KEYS.add(openKey);
                            sb.delete(close, j + 2);
                            sb.delete(open, i + 2);
                        }
                    }
                }
            }
        }
        return sb.toString();
    }
}
