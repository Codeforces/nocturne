/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.link;

import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.util.Map;

/**
 * Generates page link.
 *
 * @author Mike Mirzayanov
 */
public class LinkDirective implements TemplateDirectiveModel {
    private static final LinkDirective INSTANCE = new LinkDirective();

    @Override
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        if (!params.containsKey("name")) {
            throw new TemplateModelException("LinkDirective directive expects name parameter.");
        }

        String name = params.get("name").toString();
        params.remove("name");

        if (loopVars.length != 0) {
            throw new TemplateModelException("LinkDirective directive doesn't allow loop variables.");
        }

        if (body == null) {
            if (params.containsKey("value")) {
                String value = params.get("value").toString();
                params.remove("value");
                String a = String.format("<a href=\"%s\">%s</a>", getLink(params, name), value);
                env.getOut().write(a);
            } else {
                env.getOut().write(getLink(params, name));
            }
        } else {
            throw new TemplateModelException("Body is not expected for LinkDirective directive.");
        }
    }

    @SuppressWarnings("unchecked")
    private static String getLink(Map params, String name) {
        return Links.getLinkByMap(name, params);
    }

    /**
     * @return Returns the only directive instance.
     */
    public static LinkDirective getInstance() {
        return INSTANCE;
    }
}
