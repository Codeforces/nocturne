/*
 * Copyright 2009 Mike Mirzayanov
 */
package org.nocturne.validation;

import org.nocturne.main.ApplicationContext;

/**
 * Each validator should implement the interface.
 *
 * @author Mike Mirzayanov
 */
@SuppressWarnings("DollarSignInName")
public abstract class Validator {
    /**
     * @param value Value to be analyzed.
     * @throws ValidationException On validation error. It is good idea to pass
     *                             localized via captions value inside ValidationException,
     *                             like {@code return new ValidationException($("Field can't be empty"));}.
     */
    public abstract void run(String value) throws ValidationException;

    /**
     * @param shortcut Shortcut value.
     * @return The same as {@code ApplicationContext.getInstance().$()}.
     */
    public String $(String shortcut) {
        return ApplicationContext.getInstance().$(shortcut);
    }

    /**
     * @param shortcut Shortcut value.
     * @param args     Shortcut arguments.
     * @return The same as {@code ApplicationContext.getInstance().$()}.
     */
    @SuppressWarnings("OverloadedVarargsMethod")
    public String $(String shortcut, Object... args) {
        return ApplicationContext.getInstance().$(shortcut, args);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {}";
    }
}
