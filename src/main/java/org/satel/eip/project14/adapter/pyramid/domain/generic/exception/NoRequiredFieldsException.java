package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class NoRequiredFieldsException extends RuntimeException {
    private final String message;
    private final String processInstanceId;
    private final String entity;

    public NoRequiredFieldsException(String entity, String processInstanceId) {
        this.processInstanceId = processInstanceId;
        this.entity = entity;
        message = String.format("Unable to create {%s} entity for process instance {%s}", entity, processInstanceId);
    }
}
