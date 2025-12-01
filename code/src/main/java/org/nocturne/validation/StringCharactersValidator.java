/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

/**
 * Ensures that all characters in string are from the given string (alphabet).
 *
 * @author Mike Mirzayanov
 * @author Andrew Lazarev
 */
public class StringCharactersValidator extends Validator {
    /**
     * Alphabet.
     */
    private final String alphabet;

    /**
     * Shortcut for error message.
     */
    private final String message;

    /**
     * @param alphabet All characters from validated value will be checked to be in alphabet.
     * @param message  Error message caption shortcut.
     */
    public StringCharactersValidator(String alphabet, String message) {
        this.alphabet = alphabet;
        this.message = message;
    }

    @Override
    public void run(String value) throws ValidationException {
        for (int i = 0; i < value.length(); ++i) {
            if (alphabet.indexOf(value.charAt(i)) == -1) {
                throw new ValidationException($(message));
            }
        }
    }

    @Override
    public String toString() {
        return String.format("StringCharactersValidator {alphabet='%s'}", alphabet);
    }
}
