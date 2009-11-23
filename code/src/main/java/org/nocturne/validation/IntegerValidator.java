/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.validation;

/**
 * Validates that specified value is integer. Optionaly checks that value is in the range.
 *
 * @author Mike Mirzayanov
 */
public class IntegerValidator extends Validator {
    /** Minimal integer value. */
    private int minimalValue = Integer.MIN_VALUE;

    /** Maximal integer value. */
    private int maximalValue = Integer.MAX_VALUE;

    /** Integer validator which doesn't check range. */
    public IntegerValidator() {
    }

    /**
     * Checks that given value is in the range [minimalValue, maximalValue].
     *
     * @param minimalValue min value.
     * @param maximalValue max value.
     */
    public IntegerValidator(int minimalValue, int maximalValue) {
        this.minimalValue = minimalValue;
        this.maximalValue = maximalValue;
    }

    /**
     * @param value Value to be analyzed.
     * @throws org.nocturne.validation.ValidationException
     *          On validation error. It is good idea to pass
     *          localized via captions value inside ValidationException,
     *          like {@code return new ValidationException($("Field can't be empty"));}.
     */
    public void run(String value) throws ValidationException {
        if (value == null || !value.matches("[-0-9]+")) {
            throw new ValidationException($("Field should contain integer value"));
        }

        int numeric;

        try {
            numeric = Integer.parseInt(value);
        } catch (Exception e) {
            throw new ValidationException($("Field should contain integer value"));
        }

        if (numeric < minimalValue) {
            throw new ValidationException($("Field should be at least {0}", minimalValue));
        }

        if (numeric > maximalValue) {
            throw new ValidationException($("Field should be no more than {0}", maximalValue));
        }
    }
}
