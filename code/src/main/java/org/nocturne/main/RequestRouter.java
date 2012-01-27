/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.main;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Mirzayanov
 */
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
    Resolution route(String path, Map<String, String> parameterMap);

    /**
     * Incapsultes response from ReuestRouter: the controller class,
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
        private final Map<String, String> overrideParameters = new HashMap<String, String>();

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
            overrideParameters.put(key, value);
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
        public Map<String, String> getOverrideParameters() {
            return overrideParameters;
        }
    }
}
