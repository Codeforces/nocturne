/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

/**
 * Validators should throw this type of exceptions on validation error.
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings("DeserializableClassInSecureContext")
public class ValidationException extends Exception {
    /**
     * @param message Validation error message. Will be displayed for users.
     */
    public ValidationException(String message) {
        super(message);
    }
}
