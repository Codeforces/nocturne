/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import org.nocturne.collection.SingleEntryList;

import java.util.*;

/**
 * @author Mike Mirzayanov
 */
@SuppressWarnings("InnerClassOfInterface")
public interface RequestRouter {
    /**
     * Override this method to return Resolution instance.
     * To dispatch URLs like "/PageClass": {@code
     * <pre>
     *     return new Resolution("your.application.pages." + path.substring(1));
     * </pre>
     * }
     * <p/>
     * Should be thread-safe.
     *
     * @param path         Page path, for example "/login";
     * @param parameterMap Contains parameters (from regquest.getParametersMap()).
     * @return Resolution, containing page class and action name. Also it can add own parameters.
     */
    Resolution route(String path, Map<String, List<String>> parameterMap);

    /**
     * Incapsulates response from RequestRouter: the controller class,
     * action and override parameters.
     */
    class Resolution {
        /**
         * Controller class name.
         */
        private final String pageClassName;

        /**
         * Action name.
         */
        private final String action;

        /**
         * Parameters which will be also injected for @Parameter annotation.
         */
        private final Map<String, List<String>> overrideParameters = new HashMap<String, List<String>>();

        /**
         * @param pageClassName Controller class name.
         * @param action        Action name.
         */
        public Resolution(String pageClassName, String action) {
            this.pageClassName = pageClassName;
            this.action = action == null ? "" : action;
        }

        /**
         * @param key   Parameter name.
         * @param value Parameter value.
         */
        public void addOverrideParameter(String key, String value) {
            overrideParameters.put(key, new SingleEntryList<String>(value));
        }

        /**
         * @param key    Parameter name.
         * @param values Parameter values.
         */
        public void addOverrideParameter(String key, List<String> values) {
            overrideParameters.put(key, new ArrayList<String>(values));
        }

        /**
         * @return Controller class name.
         */
        public String getPageClassName() {
            return pageClassName;
        }

        /**
         * @return Action name or empty string if not specified.
         */
        public String getAction() {
            return action;
        }

        /**
         * @return Parameters which will be also injected for @Parameter annotation.
         */
        public Map<String, List<String>> getOverrideParameters() {
            return Collections.unmodifiableMap(overrideParameters);
        }
    }
}
