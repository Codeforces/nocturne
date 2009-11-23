/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.util.Map;

/**
 * Use code like {@code <@frame name="loginFormFrame"/>} to inject
 * frame content into some template. But frame should be parsed into
 * variable loginFormFrame on action processing phase.
 *
 * @author Mike Mirzayanov
 */
public class FrameDirective implements TemplateDirectiveModel {
    private FrameDirective() {
        // No operations.
    }

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

            String frameName = ((SimpleScalar) name).getAsString();
            String html = component.getFrameHtml(frameName);
            if (html == null) {
                throw new TemplateException("Frame directive expected parsed frame '" + frameName + "', but didn't find.", environment);
            }

            environment.getOut().write(html);
            environment.getOut().flush();
        } else {
            throw new TemplateException("Frame directive parameter 'name' should be a String.", environment);
        }
    }

    public static FrameDirective getInstance() {
        return Internal.INSTANCE;
    }

    private static class Internal {
        private static final FrameDirective INSTANCE = new FrameDirective();
    }
}
