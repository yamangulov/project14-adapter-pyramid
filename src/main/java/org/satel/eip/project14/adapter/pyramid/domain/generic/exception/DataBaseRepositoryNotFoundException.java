package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

public class DataBaseRepositoryNotFoundException extends DataBaseGenericException {

    public DataBaseRepositoryNotFoundException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public DataBaseRepositoryNotFoundException(String errorMessage) {
        super(errorMessage);
    }

    public DataBaseRepositoryNotFoundException(Throwable err) {
        super(err);
    }

    public DataBaseRepositoryNotFoundException() {
        super();
    }
}