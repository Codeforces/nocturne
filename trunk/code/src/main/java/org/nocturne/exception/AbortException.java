/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.exception;

import javax.annotation.Nullable;

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
    @Nullable
    private final String redirectionTarget;

    /**
     * @param message Abort information message (not used).
     */
    public AbortException(String message) {
        this(message, null);
    }

    /**
     * @param message           Abort information message (not used).
     * @param redirectionTarget Target URL or {@code null}.
     */
    public AbortException(String message, @Nullable String redirectionTarget) {
        super(message);
        this.redirectionTarget = redirectionTarget;
    }

    @Nullable
    public String getRedirectionTarget() {
        return redirectionTarget;
    }
}
