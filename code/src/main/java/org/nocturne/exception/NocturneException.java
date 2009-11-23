/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.exception;

/**
 * Nocturne fails. I hope you will not see it.
 *
 * @author Mike Mirzayanov
 */
public class NocturneException extends RuntimeException {
    /** @param message Error message. */
    public NocturneException(String message) {
        super(message);
    }

    /**
     * @param message Error message.
     * @param cause   Cause.
     */
    public NocturneException(String message, Throwable cause) {
        super(message, cause);
    }
}
