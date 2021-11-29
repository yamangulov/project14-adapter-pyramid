package org.satel.eip.project14.adapter.pyramid.domain.command.entity;




import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.GenericException;

import java.util.UUID;

public interface GenericCommandInterface {

    boolean syncWithDB() throws GenericException;

    boolean validateGuid(UUID guid) throws GenericException;


}
