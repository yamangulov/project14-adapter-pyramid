package org.satel.eip.project14.adapter.pyramid.domain.generic;


import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.DataBaseGenericException;

public interface GenericRepository {

    GenericRootEntity create(GenericRootEntity object) throws DataBaseGenericException;

}
