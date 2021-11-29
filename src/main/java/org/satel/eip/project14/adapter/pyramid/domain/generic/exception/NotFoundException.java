package org.satel.eip.project14.adapter.pyramid.domain.generic.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;


@Getter
@Setter
@ToString
@AllArgsConstructor
public class NotFoundException extends RuntimeException {
    private final String message;
}
