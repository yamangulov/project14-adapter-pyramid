package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

public class DataBaseGenericException extends GenericException {
    public DataBaseGenericException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public DataBaseGenericException(String errorMessage) {
        super(errorMessage);
    }


    public DataBaseGenericException(Throwable err) {
        super(err);
    }

    public DataBaseGenericException() {
        super();
    }
}