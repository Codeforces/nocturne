/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.exception;

/**
 * In case of incorrect logic (bugs) in web-application (client) code.
 * It is throwed if nocturne found broken condition and it means a error in
 * your code.
 *
 * @author Mike Mirzayanov
 */
public class IncorrectLogicException extends RuntimeException {
    /** @param message Error message. */
    public IncorrectLogicException(String message) {
        super(message);
    }

    /**
     * @param message Error message.
     * @param cause   Cause.
     */
    public IncorrectLogicException(String message, Throwable cause) {
        super(message, cause);
    }
}
