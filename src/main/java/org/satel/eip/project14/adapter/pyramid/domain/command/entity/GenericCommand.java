package org.satel.eip.project14.adapter.pyramid.domain.command.entity;



import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.GenericException;

import java.util.UUID;

public class GenericCommand implements GenericCommandInterface {

    @Override
    public boolean syncWithDB() throws GenericException {
        throw new GenericException("Command does not exist");
    }

    @Override
    public boolean validateGuid(UUID guid) throws GenericException {
        throw new GenericException("Command does not exist");
    }
}
