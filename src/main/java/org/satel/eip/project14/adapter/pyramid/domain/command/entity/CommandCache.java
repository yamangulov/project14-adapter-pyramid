package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.GenericException;

import java.util.UUID;

@Data
public class CommandCache {

    private GenericCommand command = new GenericCommand();

    public boolean validateGuid() throws GenericException {
        return command.validateGuid(UUID.randomUUID());
    }

    //todo lifecicle
    @JsonProperty("isExecuted")
    private Boolean isExecuted = false;

    @JsonProperty("isSuccessful")
    private Boolean isSuccessful = false;


}
