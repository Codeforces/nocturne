/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import org.apache.log4j.Logger;

/**
 * Validates that given value length is in specified range.
 *
 * @author Mike Mirzayanov
 */
public class LengthValidator extends Validator {
    private static final Logger logger = Logger.getLogger(LengthValidator.class);

    /**
     * Minimal length.
     */
    private int minimalLength = Integer.MIN_VALUE;

    /**
     * Maximal length.
     */
    private int maximalLength = Integer.MAX_VALUE;

    /**
     * @param minimalLength Minimal length.
     */
    public LengthValidator(int minimalLength) {
        this.minimalLength = minimalLength;
    }

    /**
     * @param minimalLength Minimal length.
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
    @Override
    public void run(String value) throws ValidationException {
        if (minimalLength >= 1 && value == null) {
            throw new ValidationException(
                    $("Field should contain at least {0,number,#} characters", minimalLength)
            );
        }

        if (minimalLength <= 0 && value == null) {
            logger.error("Value is `null` but minimalLength<=0.");
            throw new ValidationException($("Field should not be empty"));
        }

        int length = value.length();

        if (length < minimalLength) {
            throw new ValidationException(
                    $("Field should contain at least {0,number,#} characters", minimalLength)
            );
        }

        if (length > maximalLength) {
            throw new ValidationException(
                    $("Field should contain no more than {0,number,#} characters", maximalLength)
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
                "LengthValidator {minimalLength=%d, maximalLength=%d}", minimalLength, maximalLength
        );
    }
}
