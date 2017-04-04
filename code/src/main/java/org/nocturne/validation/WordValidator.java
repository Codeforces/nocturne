/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import java.util.regex.Pattern;

/**
 * Checks that validated value is a single non-empty word (i.e. matches "\\w+").
 *
 * @author Mike Mirzayanov
 */
public class WordValidator extends Validator {
    private static final Pattern WORD_PATTERN = Pattern.compile("\\w+");

    /**
     * @param value Value to be analyzed.
     * @throws ValidationException
     *          On validation error. It is good idea to pass
     *          localized via captions value inside ValidationException,
     *          like {@code return new ValidationException($("Field can't be empty"));}.
     */
    @Override
    public void run(String value) throws ValidationException {
        if (!WORD_PATTERN.matcher(value).matches()) {
            throw new ValidationException($("Field should contain letters, digits and underscore characters"));
        }
    }
}
