/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.exception;

/**
 * If can't invoke operation via reflection. Nocturne uses reflection only
 * in the debug mode.
 *
 * @author Mike Mirzayanov
 */
public class ReflectionException extends Exception {
    /**
     * @param message Error message.
     * @param cause   Cause.
     */
    public ReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

    /** @param message Error message. */
    public ReflectionException(String message) {
        super(message);
    }
}
