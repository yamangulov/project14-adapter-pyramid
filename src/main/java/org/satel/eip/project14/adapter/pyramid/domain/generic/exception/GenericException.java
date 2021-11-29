package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

public class GenericException extends Exception {
    public GenericException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public GenericException(String errorMessage) {
        super(errorMessage);
    }

    public GenericException(Throwable err) {
        super(err);
    }

    public GenericException() {
        super();
    }

    @Override
    public String toString() {
        String s = getClass().getSimpleName();
        String message = getLocalizedMessage();
        return (message != null) ? (s + ": " + message) : s;
    }
}