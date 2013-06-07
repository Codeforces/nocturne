/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import freemarker.template.TemplateException;
import org.nocturne.cache.CacheHandler;
import org.nocturne.exception.FreemarkerException;
import org.nocturne.exception.InterruptException;

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
    /**
     * @return Current processing page for current request.
     */
    public Page getCurrentPage() {
        return ApplicationContext.getInstance().getCurrentPage();
    }

    String parseTemplate() {
        prepareForAction();

        CacheHandler cacheHandler = getCacheHandler();
        String result = null;
        if (cacheHandler != null && !isSkipTemplate()) {
            result = cacheHandler.intercept(this);
        }

        try {
            if (result == null) {
                boolean interrupted = false;

                try {
                    initializeAction();
                } catch (InterruptException ignored) {
                    interrupted = true;
                }

                if (!interrupted) {
                    Events.fireBeforeAction(this);
                    try {
                        internalRunAction(getActionName());
                    } catch (InterruptException ignored) {
                        // No operations.
                    }
                    Events.fireAfterAction(this);
                }

                try {
                    finalizeAction();
                } catch (InterruptException ignored) {
                    // No operations.
                }

                if (isSkipTemplate()) {
                    return null;
                } else {
                    StringWriter writer = new StringWriter(4096);
                    Map<String, Object> params = new HashMap<String, Object>(internalGetTemplateMap());
                    params.putAll(ApplicationContext.getInstance().getCurrentPage().internalGetGlobalTemplateMap());
                    getTemplate().process(params, writer);
                    writer.close();

                    result = writer.getBuffer().toString();
                    if (cacheHandler != null) {
                        cacheHandler.postprocess(this, result);
                    }
                    return result;
                }
            } else {
                return result;
            }
        } catch (TemplateException e) {
            throw new FreemarkerException("Can't parse frame " + getClass().getSimpleName() + '.', e);
        } catch (IOException e) {
            throw new FreemarkerException("Can't parse frame " + getClass().getSimpleName() + '.', e);
        } finally {
            finalizeAfterAction();
        }
    }

    @Override
    protected void setupRequestParams() {
        setupRequestParamsForFrame();
    }
}
