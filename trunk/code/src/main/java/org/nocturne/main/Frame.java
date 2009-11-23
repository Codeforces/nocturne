/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import freemarker.template.TemplateException;
import org.nocturne.exception.FreemarkerException;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Often there are small pieces of logic+view exist. For example, panel
 * with recent news can be on many pages. You can code it as Frame. Frame is
 * a special component which injects into some page or other component and has
 * own template.
 *
 * @author Mike Mirzayanov
 */
public abstract class Frame extends Component {
    /** @return Current processing page for current request. */
    public Page getCurrentPage() {
        return ApplicationContext.getInstance().getCurrentPage();
    }

    String parseTemplate() {
        prepareForAction();

        initializeAction();
        Events.fireBeforeAction(this);
        internalRunAction(getActionName());
        Events.fireAfterAction(this);
        finalizeAction();

        try {
            StringWriter writer = new StringWriter(4096);

            Map<String, Object> params = new HashMap<String, Object>(internalGetTemplateMap());
            params.putAll(ApplicationContext.getInstance().getCurrentPage().getGlobalTemplateMap());

            getTemplate().process(params, writer);
            writer.close();

            return writer.getBuffer().toString();
        } catch (TemplateException e) {
            throw new FreemarkerException("Can't parse frame " + getClass().getSimpleName() + ".", e);
        } catch (IOException e) {
            throw new FreemarkerException("Can't parse frame " + getClass().getSimpleName() + ".", e);
        } finally {
            finalizeAfterAction();
        }
    }
}
