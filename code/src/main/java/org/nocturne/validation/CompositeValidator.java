package org.nocturne.validation;

/**
 * @author kuviman
 */
public class CompositeValidator extends Validator {
    private final Validator[] validators;

    public CompositeValidator(Validator... validators) {
        this.validators = validators;
    }

    @Override
    public void run(String value) throws ValidationException {
        for (Validator validator : validators) {
            validator.run(value);
        }
    }
}
