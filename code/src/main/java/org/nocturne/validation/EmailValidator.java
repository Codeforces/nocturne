/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import org.nocturne.util.StringUtil;

import java.util.regex.Pattern;

/**
 * Validates email. Doesn't use RFC. Use very loyal rules.
 *
 * @author Mike Mirzayanov
 */
public class EmailValidator extends Validator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^@<>&\"\'/\\\\\\s]+@[^@<>&\"\'/\\\\\\s]+\\.[a-z]+");

    /**
     * Shortcut for error message.
     */
    private final String message;

    /**
     * Creates validator with default error message: Field should contain valid email.
     */
    public EmailValidator() {
        this("Field should contain valid email");
    }

    /**
     * Creates validator with specific error message.
     * Actually caption shortcut should be passed.
     *
     * @param message Error message caption shortcut.
     */
    public EmailValidator(String message) {
        this.message = message;
    }

    @Override
    public void run(String value) throws ValidationException {
        boolean invalid = false;

        if (StringUtil.isEmptyOrNull(value)) {
            invalid = true;
        }

        if (!invalid) {
            for (int i = 0; i < value.length(); i++) {
                if (value.charAt(i) <= ' ') {
                    invalid = true;
                }
            }
        }

        if (!invalid && !EMAIL_PATTERN.matcher(value).matches()) {
            invalid = true;
        }

        if (invalid) {
            throw new ValidationException($(message));
        }
    }
}
