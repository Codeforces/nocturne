/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.caption;

import freemarker.core.Environment;
import freemarker.template.*;
import org.nocturne.main.ApplicationContext;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Generates caption value by shortcut and args. It injected to any page or frame.
 * </p>
 * Examples:
 * <pre>
 *      {@literal <@caption params=["Mike"]>Hello, {0}</@caption>}
 *      {@literal <@caption>Login</@caption>}
 *      {@literal <@caption key="Login"/>}
 *      {@literal <@caption key="Hello, {0}" params=["Mike"]/>}
 * </pre>
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings("Singleton")
public class CaptionDirective implements TemplateDirectiveModel {
    /**
     * Singleton instance.
     */
    private static final CaptionDirective INSTANCE = new CaptionDirective();

    private static final Object[] EMPTY_OBJECT_ARRAY = {};

    private CaptionDirective() {
        // No operations.
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public void execute(Environment env, Map params, TemplateModel[] loopVars, TemplateDirectiveBody body)
            throws TemplateException, IOException {
        char[] chars = null;

        if (body != null) {
            CharArrayWriter writer = new CharArrayWriter();
            body.render(writer);
            writer.close();
            chars = writer.toCharArray();
        }

        if (params.containsKey("key") && chars != null && chars.length > 0) {
            throw new TemplateModelException(
                    "Caption directive expects key parameter or directive body, but not in the same time."
            );
        }

        if (!params.containsKey("key") && (chars == null || chars.length == 0)) {
            throw new TemplateModelException(
                    "Caption directive expects either key parameter or directive body, but none found."
            );
        }

        Set<String> keys = params.keySet();

        String key;
        if (params.containsKey("key")) {
            key = params.get("key").toString();
        } else {
            key = String.valueOf(chars);
        }
        keys.remove("key");

        Object p = params.get("params");
        keys.remove("params");

        if (!keys.isEmpty()) {
            throw new TemplateModelException("Caption directive contains unexpected params.");
        }

        Object[] args = EMPTY_OBJECT_ARRAY;

        if (p instanceof SimpleSequence) {
            SimpleSequence sequence = (SimpleSequence) p;
            args = new Object[sequence.size()];
            for (int i = 0; i < sequence.size(); i++) {
                Object arg = sequence.get(i);
                if (arg instanceof SimpleNumber) {
                    args[i] = ((TemplateNumberModel) arg).getAsNumber();
                } else {
                    args[i] = arg.toString();
                }
            }
        }

        String value = ApplicationContext.getInstance().$(key, args);
        env.getOut().write(value);
    }

    /**
     * @return Returns singleton instance.
     */
    public static CaptionDirective getInstance() {
        return INSTANCE;
    }
}
