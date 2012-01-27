/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import org.nocturne.util.StringUtil;

/**
 * Validates email. Doesn't use RFC. Use very loyal rules.
 *
 * @author Mike Mirzayanov
 */
public class EmailValidator extends Validator {
    /**
     * Shortcut for error message.
     */
    private String message = "Field should contain valid email";

    /**
     * Creates validator with default error message: Field should contain valid email.
     */
    public EmailValidator() {
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

        if (!invalid && !value.matches("[^@<>&\"\'/\\\\\\s]+@[^@<>&\"\'/\\\\\\s]+\\.[a-z]+")) {
            invalid = true;
        }

        if (invalid) {
            throw new ValidationException($(message));
        }
    }
}
