package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
public class IllegalEnumException extends RuntimeException {

    private final String message;

    public IllegalEnumException(String aMessage) {
        message = aMessage;
    }
}
