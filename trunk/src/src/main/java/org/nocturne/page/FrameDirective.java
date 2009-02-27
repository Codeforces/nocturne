package org.nocturne.page;

import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.util.Map;

/** @author Mike Mirzayanov */
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
            //Component component = ComponentLocator.get(environment.getTemplate());
            Component component = ComponentLocator.getPage();
    
            String s = ((SimpleScalar) name).getAsString();
            try {
                String html = component.getFrameHtml(s);
                environment.getOut().write(html);
            } catch (Throwable e) {
                System.out.println(e);
            }
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
