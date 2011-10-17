/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.link;

import freemarker.core.Environment;
import freemarker.template.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Generates page link.
 *
 * @author Mike Mirzayanov
 */
public class LinkDirective implements TemplateDirectiveModel {
    private static final LinkDirective INSTANCE = new LinkDirective();

    private final List<Interceptor> interceptors = new CopyOnWriteArrayList<Interceptor>();

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

    private String getLink(Map params, String name) {
        //noinspection unchecked
        String link = Links.getLinkByMap(name, params);

        for (Interceptor interceptor : interceptors) {
            link = interceptor.process(link, name, params);
        }

        return link;
    }

    /**
     * Adds interceptor to the link directive.
     * Link will be processed by interceptors after all logic of this directive is completed
     * and before resulting string will be written into the output.
     *
     * @param interceptor link directive interceptor to add
     */
    public void addInterceptor(Interceptor interceptor) {
        interceptors.add(interceptor);
    }

    /**
     * @return Returns the only directive instance.
     */
    public static LinkDirective getInstance() {
        return INSTANCE;
    }

    public interface Interceptor {
        /**
         * Link directive calls this method to postprocess link.
         *
         * @param link   link to process
         * @param name   name of the link
         * @param params parameters of the link
         * @return processed link
         */
        String process(String link, String name, Map params);
    }
}
