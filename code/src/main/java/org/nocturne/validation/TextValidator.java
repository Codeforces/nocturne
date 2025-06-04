package org.nocturne.validation;

import org.apache.commons.lang3.ArrayUtils;

/**
 * Checks that parameter doesn't contain binary characters (with codes less than 9).
 *
 * @author Mike Mirzayanov
 */
public class TextValidator extends Validator {
    public static final double NON_STRICT_BINARY_DATA_MAX_RATIO = 0.1;
    /**
     * Shortcut for error message.
     */
    private final String message;

    /**
     * Parameters for message caption.
     */
    private final Object[] messageParams;

    /**
     * Strict validation disallows any binary data, non-strict allows some.
     */
    private final boolean strict;

    /**
     * Creates validator with default error message: Field can't contain binary data.
     */
    public TextValidator() {
        this(true);
    }

    /**
     * Creates validator with default error message: Field can't contain binary data.
     *
     * @param strict Strict model enable flag.
     */
    public TextValidator(boolean strict) {
        this(strict, "Field can't contain binary data", ArrayUtils.EMPTY_OBJECT_ARRAY);
    }

    /**
     * Creates validator with specific error message.
     * Actually caption shortcut should be passed.
     *
     * @param message       Error message caption shortcut.
     * @param messageParams Message shortcut parameters.
     */
    public TextValidator(String message, Object... messageParams) {
        this(true, message, messageParams);
    }

    /**
     * Creates validator with specific error message.
     * Actually caption shortcut should be passed.
     *
     * @param strict        Strict model enable flag.
     * @param message       Error message caption shortcut.
     * @param messageParams Message shortcut parameters.
     */
    public TextValidator(boolean strict, String message, Object... messageParams) {
        this.strict = strict;
        this.message = message;
        this.messageParams = messageParams;
    }

    @Override
    public void run(String value) throws ValidationException {
        int binaryCount = 0;
        for (int i = 0; i < value.length(); ++i) {
            if (value.charAt(i) < 9) {
                binaryCount++;
            }
        }
        if ((strict && binaryCount != 0) || (!strict && binaryCount > value.length() * NON_STRICT_BINARY_DATA_MAX_RATIO)) {
            throw new ValidationException($(message, messageParams));
        }
    }
}
