/* Copyright by Mike Mirzayanov. */
package org.nocturne.validation;

import org.nocturne.util.StringUtil;

/**
 * Ensures that value is not null and not empty.
 *
 * @author Mike Mirzayanov
 */
public class RequiredValidator extends Validator {
    /**
     * Shortcut for error message.
     */
    private final String message;

    /**
     * Creates validator with default error message: Field should not be empty.
     */
    public RequiredValidator() {
        this("Field should not be empty");
    }

    /**
     * Creates validator with specific error message.
     * Actually caption shortcut should be passed.
     *
     * @param message Error message caption shortcut.
     */
    public RequiredValidator(String message) {
        this.message = message;
    }

    @Override
    public void run(String value) throws ValidationException {
        if (StringUtil.isEmpty(value)) {
            throw new ValidationException($(message));
        }
    }
}
