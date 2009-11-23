/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.exception;

/**
 * On illegal freemarker configuration or state.
 *
 * @author Mike Mirzayanov
 */
public class FreemarkerException extends RuntimeException {
    /**
     * @param message Error message.
     */
    public FreemarkerException(String message) {
        super(message);
    }

    /**
     * @param message Error message.
     * @param cause Cause.
     */
    public FreemarkerException(String message, Throwable cause) {
        super(message, cause);    
    }
}
