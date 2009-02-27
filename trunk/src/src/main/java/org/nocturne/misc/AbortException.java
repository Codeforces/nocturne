package org.nocturne.misc;

/** @author Mike Mirzayanov */
public class AbortException extends RuntimeException {
    public AbortException() {
        super();
    }

    public AbortException(String message) {
        super(message);
    }

    public AbortException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbortException(Throwable cause) {
        super(cause);
    }
}
