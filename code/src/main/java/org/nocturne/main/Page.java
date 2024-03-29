/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import freemarker.template.TemplateException;
import io.prometheus.client.Summary;
import org.nocturne.cache.CacheHandler;
import org.nocturne.exception.*;
import org.nocturne.postprocess.ResponsePostprocessor;
import org.nocturne.prometheus.Prometheus;
import org.nocturne.util.ReflectionUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Main controller class. Read <a href="http://code.google.com/p/nocturne/wiki/RequestLifeCycle_RU">http://code.google.com/p/nocturne/wiki/RequestLifeCycle_RU</a>
 * or <a href="http://code.google.com/p/nocturne/wiki/RequestLifeCycle_EN">http://code.google.com/p/nocturne/wiki/RequestLifeCycle_EN</a>
 * for details of its lifecycle.
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public abstract class Page extends Component {
    /**
     * Global template variables map.
     */
    private Map<String, Object> globalTemplateMap;

    /**
     * Stores additional css resources added by addCss() from the page or internal frames.
     */
    private final Set<String> cssSet = new LinkedHashSet<>();

    /**
     * Stores additional js resources added by addJs() from the page or internal frames.
     */
    private final Set<String> jsSet = new LinkedHashSet<>();

    /**
     * Default is null, which means no postprocessing.
     */
    private ResponsePostprocessor responsePostprocessor;

    /**
     * Request-scoped cache. For internal usage.
     */
    private Map<String, Object> requestCache;

    /**
     * Flag, which stores should workflow be passed to filterChain.
     */
    private boolean processChain;

    Map<String, Object> getRequestCache() {
        return requestCache;
    }

    void setRequestCache(Map<String, Object> requestCache) {
        this.requestCache = requestCache;
    }

    /**
     * @return {@code true} iff setProcessChain(true) has been called and
     *         it means that workflow will be passed to filterChain after page processed.
     */
    public boolean isProcessChain() {
        return processChain;
    }

    /**
     * @return Request postprocessor or {@code null} if not set.
     */
    public ResponsePostprocessor getResponsePostprocessor() {
        return responsePostprocessor;
    }

    /**
     * @param responsePostprocessor Request postprocessor to postprocess response from the current page.
     */
    public void setResponsePostprocessor(ResponsePostprocessor responsePostprocessor) {
        this.responsePostprocessor = responsePostprocessor;
    }

    /**
     * @param processChain {@code true} if you want to use filterChain after
     *                     page usage.
     */
    protected void setProcessChain(boolean processChain) {
        this.processChain = processChain;
    }

    void putRequestCache(String key, Object value) {
        if (requestCache != null) {
            requestCache.put(key, value);
        }
    }

    Object getRequestCache(String key) {
        return requestCache == null ? null : requestCache.get(key);
    }

    void removeRequestCache(String key) {
        if (requestCache != null) {
            requestCache.remove(key);
        }
    }

    Set<String> getCssSet() {
        return cssSet;
    }

    Set<String> getJsSet() {
        return jsSet;
    }

    public Map<String, Object> getGlobalTemplateMap() {
        return globalTemplateMap == null ? null : new HashMap<>(globalTemplateMap);
    }

    Map<String, Object> internalGetGlobalTemplateMap() {
        return globalTemplateMap;
    }

    /**
     * Handles main part of page workflow and parses template (writes it to response) if needed.
     */
    public void parseTemplate() {
        String simpleClassName = ReflectionUtil.getOriginalClass(getClass()).getSimpleName();

        Prometheus.getPagesCounter().labels(simpleClassName).inc();
        Summary.Timer overallTimer = Prometheus.getPagesLatencySeconds()
                .labels(simpleClassName, "overall").startTimer();

        try {
            prepareForAction();

            CacheHandler cacheHandler = getCacheHandler();
            String result = null;
            if (cacheHandler != null && !isSkipTemplate()) {
                result = cacheHandler.intercept(this);
                result = handleRequestPostprocessor(result);
            }

            if (result == null) {
                boolean interrupted = false;

                Summary.Timer initializeActionTimer = Prometheus.getPagesLatencySeconds()
                        .labels(simpleClassName, "initializeAction").startTimer();
                try {
                    initializeAction();
                } catch (InterruptException e) {
                    interrupted = true;
                } finally {
                    initializeActionTimer.observeDuration();
                }

                if (!interrupted) {
                    // Before action.
                    {
                        Summary.Timer beforeActionTimer = Prometheus.getPagesLatencySeconds()
                                .labels(simpleClassName, "beforeAction").startTimer();
                        try {
                            Events.fireBeforeAction(this);
                        } finally {
                            beforeActionTimer.observeDuration();
                        }
                    }

                    // Action.
                    {
                        Summary.Timer actionTimer = Prometheus.getPagesLatencySeconds()
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
                        Summary.Timer afterActionTimer = Prometheus.getPagesLatencySeconds()
                                .labels(simpleClassName, "afterAction").startTimer();
                        try {
                            Events.fireAfterAction(this);
                        } finally {
                            afterActionTimer.observeDuration();
                        }
                    }
                }

                Summary.Timer finalizeActionTimer = Prometheus.getPagesLatencySeconds()
                        .labels(simpleClassName, "finalizeAction").startTimer();
                try {
                    finalizeAction();
                } catch (InterruptException ignored) {
                    // No operations.
                } finally {
                    finalizeActionTimer.observeDuration();
                }

                if (!isSkipTemplate()) {
                    Map<String, Object> params = new HashMap<>(internalGetTemplateMap());
                    params.putAll(internalGetGlobalTemplateMap());

                    Summary.Timer templateTimer = Prometheus.getPagesLatencySeconds()
                            .labels(simpleClassName, "template").startTimer();
                    try {
                        getTemplate().setOutputEncoding(StandardCharsets.UTF_8.name());

                        StringWriter stringWriter = new StringWriter(65536);
                        getTemplate().process(params, stringWriter);
                        stringWriter.close();

                        result = ((FrameDirective) getGlobalTemplateMap().get("frame")).processComponentUniques(
                                new StringBuilder(stringWriter.getBuffer()));

                        if (cacheHandler != null) {
                            cacheHandler.postprocess(this, result);
                        }

                        result = handleRequestPostprocessor(result);
                    } catch (TemplateException e) {
                        throw new FreemarkerException("Can't parse template for page " + getClass().getName() + '.', e);
                    } catch (IOException e) {
                        if (!e.toString().contains("ClientAbortException")) {
                            throw new FreemarkerException("Can't parse template for page " + getClass().getName() + '.', e);
                        }
                    } finally {
                        templateTimer.observeDuration();
                    }
                }
            }

            if (result != null) {
                getOutputStream().write(result.getBytes(StandardCharsets.UTF_8));
            }
        } catch (AbortException ignored) {
            // No operations.
        } catch (IOException e) {
            throw new FreemarkerException("Can't write page " + getClass().getName() + '.', e);
        } finally {
            finalizeAfterAction();
            overallTimer.observeDuration();
        }
    }

    void finalizeAfterAction() {
        requestCache = null;
        globalTemplateMap = null;
        super.finalizeAfterAction();
    }

    private String handleRequestPostprocessor(String result) {
        ResponsePostprocessor postprocessor = responsePostprocessor;
        if (postprocessor != null) {
            result = postprocessor.postprocess(this, result);
        }
        return result;
    }

    @Override
    void prepareForAction() {
        jsSet.clear();
        cssSet.clear();

        setupCurrentPage();

        globalTemplateMap = Collections.synchronizedMap(new HashMap<>());
        requestCache = Collections.synchronizedMap(new HashMap<>());

        super.prepareForAction();

        internalGetGlobalTemplateMap().put("frame", new FrameDirective());
        internalGetGlobalTemplateMap().put("once", new OnceDirective());

        internalGetTemplateMap().put("css", cssSet);
        internalGetTemplateMap().put("js", jsSet);

        processChain = false;
    }

    @Override
    protected final void setupRequestParams() {
        setupRequestParamsForPage();
    }

    private void setupCurrentPage() {
        if (ApplicationContext.getInstance().isDebug()) {
            try {
                ReflectionUtil.invoke(ApplicationContext.getInstance(), "setCurrentPage", this);
            } catch (ReflectionException e) {
                throw new NocturneException("Can't set current page.", e);
            }
        } else {
            ApplicationContext.getInstance().setCurrentPage(this);
        }
    }
}
