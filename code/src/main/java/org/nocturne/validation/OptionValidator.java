/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import java.util.Arrays;

/**
 * Validates that given value equals to one object from the given list.
 * Uses object.toString().equals(value) to check equality.
 *
 * @author Mike Mirzayanov
 */
public class OptionValidator extends Validator {
    /**
     * Possible options.
     */
    private final Object[] options;

    /**
     * Constructs validator with given list of possible options.
     *
     * @param options List of options.
     */
    public OptionValidator(Object... options) {
        this.options = options;
    }

    /**
     * @param value Value to be analyzed.
     * @throws ValidationException On validation error. It is good idea to pass
     *                             localized via captions value inside ValidationException,
     *                             like {@code return new ValidationException($("Field can't be empty"));}.
     */
    @Override
    public void run(String value) throws ValidationException {
        if (value == null) {
            for (Object option : options) {
                if (option == null) {
                    return;
                }
            }
        } else {
            for (Object option : options) {
                if (option != null && option.toString().equals(value)) {
                    return;
                }
            }
        }

        throw new ValidationException($("Field contains unexpected value"));
    }

    @Override
    public String toString() {
        return String.format("OptionValidator {options=%s}", Arrays.toString(options));
    }
}
