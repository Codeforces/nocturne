/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import java.util.regex.Pattern;

/**
 * Ensures that the given value is matched by specified regex pattern.
 *
 * @author Mike Mirzayanov
 */
public class PatternValidator extends Validator {
    /**
     * Regex pattern.
     */
    private final Pattern pattern;

    /**
     * Shortcut for error message.
     */
    private final String message;

    /**
     * @param pattern Validated strings will be checked to match the given pattern.
     * @param message Error message caption shortcut.
     */
    public PatternValidator(String pattern, String message) {
        this.pattern = Pattern.compile(pattern);
        this.message = message;
    }

    @Override
    public void run(String value) throws ValidationException {
        if (!pattern.matcher(value).matches()) {
            throw new ValidationException($(message));
        }
    }

    @Override
    public String toString() {
        return String.format("PatternValidator {pattern='%s'}", pattern);
    }
}
