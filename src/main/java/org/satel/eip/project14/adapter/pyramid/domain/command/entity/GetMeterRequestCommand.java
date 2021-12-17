package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.GenericException;

import java.util.UUID;


@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetMeterRequestCommand extends GenericCommand {

    @JsonProperty("body")
    private GetMeterRequest body;

    @JsonProperty("commandType")
    private String commandType;

    @JsonIgnore
    private ConnectionInformation connectionInformation = null;

    @JsonProperty("uuid")
    private UUID uuid;

    @Override
    public boolean syncWithDB() throws GenericException {
        return true;
    }

    @Override
    public boolean validateGuid(UUID guid) throws GenericException {
        //implement check
        return true;
    }
}
