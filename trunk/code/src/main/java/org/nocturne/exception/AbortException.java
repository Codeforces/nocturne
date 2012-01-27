/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.exception;

/**
 * Application will throw this exception on abort of execution
 * (usually on redirect).
 * <p/>
 * Do not throw this exception directly, but use methods like abort
 * abortWithRedirect().
 *
 * @author Mike Mirzayanov
 */
public class AbortException extends RuntimeException {
    /**
     * @param message Abort information message (not used).
     */
    public AbortException(String message) {
        super(message);
    }
}
