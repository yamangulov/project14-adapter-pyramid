package org.satel.eip.project14.adapter.pyramid.domain.generic;


import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.DataBaseGenericException;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.GenericException;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.ObjectGenericException;

import java.util.UUID;

public interface GenericRootEntity {

    void compliance() throws GenericException;

    UUID getUuid();

    void verifyDatabaseIdentity() throws DataBaseGenericException;

    void verifyObjectIdentity() throws ObjectGenericException;
}
