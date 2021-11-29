package org.satel.eip.project14.adapter.pyramid.domain.command.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.GenericException;

import java.util.UUID;


@Data
@EqualsAndHashCode(callSuper = true)
public class GetMeterRequestCommand extends GenericCommand {

    private GetMeterRequest body;

    private String commandType;

    private ConnectionInformation connectionInformation = null;

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
