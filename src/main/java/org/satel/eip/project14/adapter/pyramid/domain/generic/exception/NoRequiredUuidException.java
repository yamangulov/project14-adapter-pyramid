package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class NoRequiredUuidException extends RuntimeException {
    private final String message;
    private final String processInstanceId;

    public NoRequiredUuidException(String processInstanceId) {
        this.processInstanceId = processInstanceId;
        message = String.format("Unable to find required uuid in camunda context for process instance {%s}", processInstanceId);
    }
}
