/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.exception;

/**
 * On illegal application configuration.
 *
 * @author Mike Mirzayanov
 */
public class ConfigurationException extends RuntimeException {
    /** @param message Error message. */
    public ConfigurationException(String message) {
        super(message);
    }

    /**
     * @param message Error message.
     * @param cause   Cause.
     */
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
