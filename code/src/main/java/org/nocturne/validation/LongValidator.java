/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import java.util.regex.Pattern;

/**
 * Validates that specified value is long. Optionaly checks that value is in the range.
 *
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 07.11.11
 */
public class LongValidator extends Validator {
    private static final Pattern LONG_MATCH_PATTERN = Pattern.compile("[\\-]?[0-9]+");

    /**
     * Minimal long value.
     */
    private long minimalValue = Long.MIN_VALUE;

    /**
     * Maximal long value.
     */
    private long maximalValue = Long.MAX_VALUE;

    /**
     * Long validator which doesn't check range.
     */
    public LongValidator() {
    }

    /**
     * Checks that given value is in the range [minimalValue, maximalValue].
     *
     * @param minimalValue min value.
     * @param maximalValue max value.
     */
    public LongValidator(long minimalValue, long maximalValue) {
        this.minimalValue = minimalValue;
        this.maximalValue = maximalValue;
    }

    /**
     * @param value Value to be analyzed.
     * @throws ValidationException On validation error. It is good idea to pass
     *                             localized via captions value inside ValidationException,
     *                             like {@code return new ValidationException($("Field can't be empty"));}.
     */
    @Override
    public void run(String value) throws ValidationException {
        if (value == null || !LONG_MATCH_PATTERN.matcher(value).matches()) {
            throw new ValidationException($("Field should contain long integer value"));
        }

        long numeric;

        try {
            numeric = Long.parseLong(value);
        } catch (Exception ignored) {
            throw new ValidationException($("Field should contain long integer value"));
        }

        if (numeric < minimalValue) {
            throw new ValidationException($("Field should be at least {0,number,#}", minimalValue));
        }

        if (numeric > maximalValue) {
            throw new ValidationException($("Field should be no more than {0,number,#}", maximalValue));
        }
    }
}
