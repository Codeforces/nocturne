/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.validation;

/**
 * Validates that given value length is in specified range.
 *
 * @author Mike Mirzayanov
 */
public class LengthValidator extends Validator {
    /** Minimal length. */
    private int minimalLength = Integer.MIN_VALUE;

    /** Maximal length. */
    private int maximalLength = Integer.MAX_VALUE;

    /** @param minimalLength Minimal length. */
    public LengthValidator(int minimalLength) {
        this.minimalLength = minimalLength;
    }

    /**
     * @param minimalLength Mimimal length.
     * @param maximalLength Maximal length.
     */
    public LengthValidator(int minimalLength, int maximalLength) {
        this.minimalLength = minimalLength;
        this.maximalLength = maximalLength;
    }

    /**
     * @param value Value to be analyzed.
     * @throws org.nocturne.validation.ValidationException
     *          On validation error. It is good idea to pass
     *          localized via captions value inside ValidationException,
     *          like {@code return new ValidationException($("Field can't be empty"));}.
     */
    public void run(String value) throws ValidationException {
        int length = value.length();

        if (length < minimalLength) {
            throw new ValidationException(
                    $("Field should contain at least {0} characters", minimalLength)
            );
        }

        if (length > maximalLength) {
            throw new ValidationException(
                    $("Field should contain no more than {0} characters", maximalLength)
            );
        }
    }
}
