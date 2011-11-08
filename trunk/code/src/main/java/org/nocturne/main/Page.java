/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.main;

import freemarker.template.TemplateException;
import org.nocturne.cache.CacheHandler;
import org.nocturne.exception.AbortException;
import org.nocturne.exception.FreemarkerException;
import org.nocturne.exception.NocturneException;
import org.nocturne.exception.ReflectionException;
import org.nocturne.util.ReflectionUtil;
import org.nocturne.postprocess.ResponsePostprocessor;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
    private Map<String, Object> globalTemplateMap = new HashMap<String, Object>();

    /**
     * Stores additional css resources added by addCss() from the page or internal frames.
     */
    private Set<String> cssSet = new HashSet<String>();

    /**
     * Stores additional js resources added by addJs() from the page or internal frames.
     */
    private Set<String> jsSet = new HashSet<String>();

    /**
     * Default is null, which means no postprocessing.
     */
    private ResponsePostprocessor responsePostprocessor;

    /**
     * Request-scoped cache. For internal usage.
     */
    private Map<String, Object> requestCache = new HashMap<String, Object>();

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
            }

            if (result == null) {
                initializeAction();
                Events.fireBeforeAction(this);
                internalRunAction(getActionName());
                Events.fireAfterAction(this);
                finalizeAction();

                if (!isSkipTemplate()) {
                    Map<String, Object> params = new HashMap<String, Object>(internalGetTemplateMap());
                    params.putAll(internalGetGlobalTemplateMap());

                    try {
                        getTemplate().setOutputEncoding("UTF-8");
                        if (cacheHandler == null) {
                            getTemplate().process(params, getWriter());
                        } else {
                            StringWriter stringWriter = new StringWriter(4096);
                            getTemplate().process(params, stringWriter);
                            stringWriter.close();

                            result = stringWriter.getBuffer().toString();
                            cacheHandler.postprocess(this, result);
                            ResponsePostprocessor postprocessor = responsePostprocessor;
                            if (postprocessor != null) {
                                result = postprocessor.postprocess(result);
                            }
                            getOutputStream().write(result.getBytes("UTF-8"));
                        }
                    } catch (TemplateException e) {
                        throw new FreemarkerException("Can't parse template for page " + getClass().getName() + '.', e);
                    } catch (IOException e) {
                        if (!e.toString().contains("ClientAbortException")) {
                            throw new FreemarkerException("Can't parse template for page " + getClass().getName() + '.', e);
                        }
                    }
                }
            } else {
                getOutputStream().write(result.getBytes("UTF-8"));
            }
        } catch (AbortException ignored) {
            // No operations.
        } catch (IOException e) {
            throw new FreemarkerException("Can't write page " + getClass().getName() + '.', e);
        }  finally {
            finalizeAfterAction();
        }
    }

    @Override
    void prepareForAction() {
        setupCurrentPage();
        super.prepareForAction();

        put("css", cssSet);
        put("js", jsSet);

        requestCache.clear();
        processChain = false;

        internalGetGlobalTemplateMap().clear();
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
