/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.exception;

/**
 * If nocturne can't initialize module. Possibly, the module
 * configuration has errors.
 *
 * @author Mike Mirzayanov
 */
public class ModuleInitializationException extends RuntimeException {
    /**
     * @param message Error message.
     */
    public ModuleInitializationException(String message) {
        super(message);
    }

    /**
     * @param message Error message.
     * @param cause   Cause.
     */
    public ModuleInitializationException(String message, Throwable cause) {
        super(message, cause);
    }
}