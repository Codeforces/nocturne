package org.nocturne.validation;

import org.apache.commons.lang.ArrayUtils;

/**
 * Checks that parameter doesn't contain binary characters (with codes less than 9).
 *
 * @author Mike Mirzayanov
 */
public class TextValidator extends Validator {
    /**
     * Shortcut for error message.
     */
    private final String message;

    /**
     * Parameters for message caption.
     */
    private final Object[] messageParams;

    /**
     * Creates validator with default error message: Field can't contain binary data.
     */
    public TextValidator() {
        this("Field can't contain binary data", ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Creates validator with specific error message.
     * Actually caption shortcut should be passed.
     *
     * @param message Error message caption shortcut.
     */
    public TextValidator(String message, Object... messageParams) {
        this.message = message;
        this.messageParams = messageParams;
    }

    @Override
    public void run(String value) throws ValidationException {
        for (int i = 0; i < value.length(); ++i) {
            if (value.charAt(i) < 9) {
                throw new ValidationException($(message, messageParams));
            }
        }
    }
}
