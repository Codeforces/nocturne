package org.nocturne.page;

import freemarker.template.TemplateException;
import org.nocturne.misc.AbortException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** @author Mike Mirzayanov */
public abstract class Page extends Component {
    private Map<String, Object> globalTemplateMap = new HashMap<String, Object>();

    private Set<String> cssSet = new HashSet<String>();

    private Set<String> jsSet = new HashSet<String>();

    private Map<String, Object> requestCache = new HashMap<String, Object>();

    private boolean processChain;

    public Map<String, Object> getRequestCache() {
        return requestCache;
    }

    public void setRequestCache(Map<String, Object> requestCache) {
        this.requestCache = requestCache;
    }

    public boolean isProcessChain() {
        return processChain;
    }

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

    Map<String, Object> getGlobalTemplateMap() {
        return globalTemplateMap;
    }

    public void parseTemplate() throws IOException {
        try {
            prepareForRender();
            beforeRender();
            render();
            afterRender();

            if (!isSkipTemplate()) {
                try {
                    Map<String, Object> params = new HashMap<String, Object>(getTemplateMap());
                    params.putAll(getGlobalTemplateMap());

                    getTemplate().process(params, getWriter());
                } catch (TemplateException e) {
                    throw new IllegalStateException(e);
                }
            }
        } catch (AbortException e) {
            // No operations.
        }
    }

    void prepareForRender() {
        super.prepareForRender();

        put("css", getCssSet());
        put("js", getJsSet());

        requestCache.clear();

        getGlobalTemplateMap().clear();
    }
}
