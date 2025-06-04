/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import java.util.regex.Pattern;

/**
 * Validates that specified value is integer. Optionaly checks that value is in the range.
 *
 * @author Mike Mirzayanov
 */
public class IntegerValidator extends Validator {
    private static final Pattern INTEGER_MATCH_PATTERN = Pattern.compile("[\\-]?[0-9]+");

    /**
     * Minimal integer value.
     */
    private int minimalValue = Integer.MIN_VALUE;

    /**
     * Maximal integer value.
     */
    private int maximalValue = Integer.MAX_VALUE;

    /**
     * Integer validator which doesn't check range.
     */
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
     * @throws ValidationException
     *          On validation error. It is good idea to pass
     *          localized via captions value inside ValidationException,
     *          like {@code return new ValidationException($("Field can't be empty"));}.
     */
    @Override
    public void run(String value) throws ValidationException {
        if (value == null || !INTEGER_MATCH_PATTERN.matcher(value).matches()) {
            throw new ValidationException($("Field should contain integer value"));
        }

        int numeric;

        try {
            numeric = Integer.parseInt(value);
        } catch (Exception ignored) {
            throw new ValidationException($("Field should contain integer value"));
        }

        if (numeric < minimalValue) {
            throw new ValidationException($("Field should be at least {0,number,#}", minimalValue));
        }

        if (numeric > maximalValue) {
            throw new ValidationException($("Field should be no more than {0,number,#}", maximalValue));
        }
    }
}
