package org.nocturne.validation;

/**
 * Checks that parameter doesn't contain binary characters (with codes less than 9).
 *
 * @author Mike Mirzayanov
 */
public class TextValidator extends Validator {
    public void run(String value) throws ValidationException {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) < 9) {
                throw new ValidationException($("Field can't contain binary data"));
            }
        }
    }
}
