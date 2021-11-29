package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

public class ComplianceException extends ObjectGenericException {

    public ComplianceException(String errorMessage, Throwable err) {
        super(errorMessage, err);
    }

    public ComplianceException(String errorMessage) {
        super(errorMessage);
    }

    public ComplianceException(Throwable err) {
        super(err);
    }

    public ComplianceException() {
        super();
    }

}
