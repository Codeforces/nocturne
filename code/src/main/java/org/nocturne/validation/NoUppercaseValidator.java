package org.nocturne.validation;

/**
 * Validates that parameter doesn't contain uppercase letters.
 *
 * @author Mike Mirzayanov
 */
public class NoUppercaseValidator extends Validator {
    @Override
    public void run(String value) throws ValidationException {
        for (int i = 0; i < value.length(); ++i) {
            if (Character.isUpperCase(value.charAt(i))) {
                throw new ValidationException($("Field can't contain uppercase letters"));
            }
        }
    }
}
