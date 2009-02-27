package org.nocturne.page.validation;

/** @author Mike Mirzayanov */
public abstract class Validator {
    public static final Validator REQUIRED = new Validator() {
        @Override
        public void run(String value) throws ValidationException {
            if (value == null || value.length() == 0) {
                throw new ValidationException("Field can't be empty");
            }
        }
    };

    public static final Validator EMAIL = new Validator() {
        public void run(String value) throws ValidationException {
            if (value == null || !value.matches("[^@]+@[^@]+\\.[a-z]+")) {
                throw new ValidationException("Field should contain valid email");
            }
        }
    };

    abstract public void run(String value) throws ValidationException;
}
