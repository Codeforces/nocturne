/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import freemarker.template.TemplateException;
import org.nocturne.cache.CacheHandler;
import org.nocturne.exception.*;
import org.nocturne.postprocess.ResponsePostprocessor;
import org.nocturne.util.ReflectionUtil;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

/**
 * Main controller class. Read <a href="http://code.google.com/p/nocturne/wiki/RequestLifeCycle_RU">http://code.google.com/p/nocturne/wiki/RequestLifeCycle_RU</a>
 * or <a href="http://code.google.com/p/nocturne/wiki/RequestLifeCycle_EN">http://code.google.com/p/nocturne/wiki/RequestLifeCycle_EN</a>
 * for details of its lifecycle.
 *
 * @author Mike Mirzayanov
 */
public abstract class Page extends Component {
    /**
     * Global template variables map.
     */
    private Map<String, Object> globalTemplateMap;

    /**
     * Stores additional css resources added by addCss() from the page or internal frames.
     */
    private final Set<String> cssSet = new HashSet<String>();

    /**
     * Stores additional js resources added by addJs() from the page or internal frames.
     */
    private final Set<String> jsSet = new HashSet<String>();

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
        requestCache.put(key, value);
    }

    Object getRequestCache(String key) {
        return requestCache.get(key);
    }

    void removeRequestCache(String key) {
        requestCache.remove(key);
    }

    Set<String> getCssSet() {
        return cssSet;
    }

    Set<String> getJsSet() {
        return jsSet;
    }

    public Map<String, Object> getGlobalTemplateMap() {
        return new HashMap<String, Object>(globalTemplateMap);
    }

    Map<String, Object> internalGetGlobalTemplateMap() {
        return globalTemplateMap;
    }

    /**
     * Handles main part of page workflow and parses template (writes it to response) if needed.
     */
    public void parseTemplate() {
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

                try {
                    initializeAction();
                } catch (InterruptException e) {
                    interrupted = true;
                }

                if (!interrupted) {
                    Events.fireBeforeAction(this);
                    try {
                        internalRunAction(getActionName());
                    } catch (InterruptException e) {
                        // No operations.
                    }
                    Events.fireAfterAction(this);
                }

                try {
                    finalizeAction();
                } catch (InterruptException e) {
                    // No operations.
                }

                if (!isSkipTemplate()) {
                    Map<String, Object> params = new HashMap<String, Object>(internalGetTemplateMap());
                    params.putAll(internalGetGlobalTemplateMap());

                    try {
                        getTemplate().setOutputEncoding("UTF-8");

                        StringWriter stringWriter = new StringWriter(65536);
                        getTemplate().process(params, stringWriter);
                        stringWriter.close();
                        result = stringWriter.getBuffer().toString();

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
                    }
                }
            }

            if (result != null) {
                getOutputStream().write(result.getBytes("UTF-8"));
            }
        } catch (AbortException ignored) {
            // No operations.
        } catch (IOException e) {
            throw new FreemarkerException("Can't write page " + getClass().getName() + '.', e);
        } finally {
            finalizeAfterAction();
        }
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

        globalTemplateMap = Collections.synchronizedMap(new HashMap<String, Object>());
        requestCache = Collections.synchronizedMap(new HashMap<String, Object>());

        super.prepareForAction();

        put("css", cssSet);
        put("js", jsSet);

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
