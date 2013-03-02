package org.nocturne.exception;

/**
 * Use it to interrupt action method of Component. It differs from AbortException
 * that the component will be rendered using the template (if no skipTemplate() was called).
 *
 * It is preferred to use interrupt() method instead of throwing it manually.
 *
 * @author Mike Mirzayanov
 */
public class InterruptException extends RuntimeException {
    public InterruptException() {
    }

    public InterruptException(String message) {
        super(message);
    }

    public InterruptException(String message, Throwable cause) {
        super(message, cause);
    }

    public InterruptException(Throwable cause) {
        super(cause);
    }
}
