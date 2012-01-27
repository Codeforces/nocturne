/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.exception;

/**
 * IOExceptions around servlets are wrapped by this exception.
 * And something other servlet-like exceptions are wrapped by it.
 *
 * @author Mike Mirzayanov
 */
public class ServletException extends RuntimeException {
    /**
     * @param message Error message.
     */
    public ServletException(String message) {
        super(message);
    }

    /**
     * @param message Error message.
     * @param cause   Cause.
     */
    public ServletException(String message, Throwable cause) {
        super(message, cause);
    }
}
