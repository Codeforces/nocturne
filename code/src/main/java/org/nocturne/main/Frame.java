/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import freemarker.template.TemplateException;
import io.prometheus.client.Summary;
import org.jetbrains.annotations.Nullable;
import org.nocturne.cache.CacheHandler;
import org.nocturne.exception.FreemarkerException;
import org.nocturne.exception.InterruptException;
import org.nocturne.prometheus.Prometheus;
import org.nocturne.util.ReflectionUtil;

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
@SuppressWarnings("WeakerAccess")
public abstract class Frame extends Component {
    /**
     * @return Current processing page for current request.
     */
    @SuppressWarnings("unused")
    public Page getCurrentPage() {
        return ApplicationContext.getInstance().getCurrentPage();
    }

    public String parseTemplate() {
        String simpleClassName = ReflectionUtil.getOriginalClass(getClass()).getSimpleName();

        Prometheus.getFramesCounter().labels(simpleClassName).inc();
        Summary.Timer overallTimer = Prometheus.getFramesLatencySeconds()
                .labels(simpleClassName, "overall").startTimer();

        try {
            return internalParseTemplate(simpleClassName);
        } finally {
            overallTimer.observeDuration();
        }
    }

    @Nullable
    private String internalParseTemplate(String simpleClassName) {
        prepareForAction();

        CacheHandler cacheHandler = getCacheHandler();
        String result = null;
        if (cacheHandler != null && !isSkipTemplate()) {
            result = cacheHandler.intercept(this);
        }

        try {
            if (result == null) {
                boolean interrupted = false;

                Summary.Timer initializeActionTimer = Prometheus.getFramesLatencySeconds()
                        .labels(simpleClassName, "initializeAction").startTimer();
                try {
                    initializeAction();
                } catch (InterruptException ignored) {
                    interrupted = true;
                } finally {
                    initializeActionTimer.observeDuration();
                }

                if (!interrupted) {
                    // Before action.
                    {
                        Summary.Timer beforeActionTimer = Prometheus.getFramesLatencySeconds()
                                .labels(simpleClassName, "beforeAction").startTimer();
                        try {
                            Events.fireBeforeAction(this);
                        } finally {
                            beforeActionTimer.observeDuration();
                        }
                    }

                    // Action.
                    {
                        Summary.Timer actionTimer = Prometheus.getFramesLatencySeconds()
                                .labels(simpleClassName, "action").startTimer();
                        try {
                            internalRunAction(getActionName());
                        } catch (InterruptException ignored) {
                            // No operations.
                        } finally {
                            actionTimer.observeDuration();
                        }
                    }

                    // After action.
                    {
                        Summary.Timer afterActionTimer = Prometheus.getFramesLatencySeconds()
                                .labels(simpleClassName, "afterAction").startTimer();
                        try {
                            Events.fireAfterAction(this);
                        } finally {
                            afterActionTimer.observeDuration();
                        }
                    }
                }

                Summary.Timer finalizeActionTimer = Prometheus.getFramesLatencySeconds()
                        .labels(simpleClassName, "finalizeAction").startTimer();
                try {
                    finalizeAction();
                } catch (InterruptException ignored) {
                    // No operations.
                } finally {
                    finalizeActionTimer.observeDuration();
                }

                if (isSkipTemplate()) {
                    return null;
                } else {
                    StringWriter writer = new StringWriter(4096);
                    Map<String, Object> params = new HashMap<>(internalGetTemplateMap());
                    params.putAll(ApplicationContext.getInstance().getCurrentPage().internalGetGlobalTemplateMap());

                    Summary.Timer templateTimer = Prometheus.getFramesLatencySeconds()
                            .labels(simpleClassName, "template").startTimer();
                    try {
                        getTemplate().process(params, writer);
                        writer.close();

                        result = writer.getBuffer().toString();
                        if (cacheHandler != null) {
                            cacheHandler.postprocess(this, result);
                        }
                        return result;
                    } finally {
                        templateTimer.observeDuration();
                    }
                }
            } else {
                return result;
            }
        } catch (TemplateException | IOException e) {
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
