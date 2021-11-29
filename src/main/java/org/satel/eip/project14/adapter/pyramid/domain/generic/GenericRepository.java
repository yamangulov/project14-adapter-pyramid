package org.satel.eip.project14.adapter.pyramid.domain.generic;


import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.DataBaseGenericException;

public interface GenericRepository {
    /*
     * CRUD
    */
    //todo остальные методы

    abstract GenericRootEntity create(GenericRootEntity object) throws DataBaseGenericException;

    //abstract <T> T create(T object) throws DataBaseGenericException;
}
