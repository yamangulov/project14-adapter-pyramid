package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

public class DatabaseQueryCreateException extends DataBaseQueryException {
    public DatabaseQueryCreateException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public DatabaseQueryCreateException(String errorMessage) {
        super(errorMessage);
    }

    public DatabaseQueryCreateException(Throwable err) {
        super(err);
    }

    public DatabaseQueryCreateException() {
        super();
    }
}
