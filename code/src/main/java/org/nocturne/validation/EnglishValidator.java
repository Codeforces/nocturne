/* Copyright by Mike Mirzayanov. */

package org.nocturne.validation;

/**
 * Ensures that specified value is English.
 * Checks that value doesn't contain characters with codes less than 9 and
 * doesn't contain too many non-ascii characters.
 *
 * @author Mike Mirzayanov
 */
public class EnglishValidator extends Validator {
    private String message = null;

    public EnglishValidator() {
    }

    public EnglishValidator(String message) {
        this.message = message;
    }

    public void run(String value) throws ValidationException {
        if (value != null) {
            char[] chars = value.toCharArray();

            int specialCount = 0;
            int nonAsciiCount = 0;

            for (char c : chars) {
                if (c < 9) {
                    specialCount++;
                }
                if (c > 127) {
                    nonAsciiCount++;
                }
            }

            if (specialCount > 0 || nonAsciiCount > value.length() / 2) {
                String msg = message != null ?
                        message : $("Field should contain value in English");
                throw new ValidationException(msg);
            }
        }
    }
}
