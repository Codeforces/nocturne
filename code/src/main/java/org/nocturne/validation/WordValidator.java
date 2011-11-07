/*
 * Copyright 2009 Mike Mirzayanov
 */

package org.nocturne.validation;

/** 
 * Checks that validated value is a single non-empty word (i.e. matches "\\w+").
 *
 * @author Mike Mirzayanov */
public class WordValidator extends Validator {
    /**
     * @param value Value to be analyzed.
     * @throws org.nocturne.validation.ValidationException On validation error. It is good idea to pass
     *                             localized via captions value inside ValidationException,
     *                             like {@code return new ValidationException($("Field can't be empty"));}.
     */
    @Override
    public void run(String value) throws ValidationException {
        if (!value.matches("\\w+")) {
            throw new ValidationException($("Field should contain letters, digits and underscore characters"));
        }
    }
}
