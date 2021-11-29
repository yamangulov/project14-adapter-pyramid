package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

public class DataBaseQueryNotFoundException extends DataBaseQueryException {
    public DataBaseQueryNotFoundException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public DataBaseQueryNotFoundException(String errorMessage) {
        super(errorMessage);
    }

    public DataBaseQueryNotFoundException(Throwable err) {
        super(err);
    }

    public DataBaseQueryNotFoundException() {
        super();
    }
}