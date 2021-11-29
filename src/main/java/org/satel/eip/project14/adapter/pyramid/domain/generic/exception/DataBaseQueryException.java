package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

public class DataBaseQueryException extends DataBaseGenericException {
    public DataBaseQueryException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public DataBaseQueryException(String errorMessage) {
        super(errorMessage);
    }

    public DataBaseQueryException(Throwable err) {
        super(err);
    }

    public DataBaseQueryException() {
        super();
    }
}