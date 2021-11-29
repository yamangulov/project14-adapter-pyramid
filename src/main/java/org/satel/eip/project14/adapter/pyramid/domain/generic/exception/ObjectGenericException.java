package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;


public class ObjectGenericException extends GenericException {
    public ObjectGenericException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ObjectGenericException(String errorMessage) {
        super(errorMessage);
    }

    public ObjectGenericException(Throwable err) {
        super(err);
    }

    public ObjectGenericException() {
        super();
    }
}
