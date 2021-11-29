package org.satel.eip.project14.adapter.pyramid.domain.generic;


import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.DataBaseGenericException;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.DataBaseRepositoryNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class GenericService {

    private GenericMapper genericMapper;



    @SneakyThrows
    public GenericRootEntity create(GenericRootEntity entity) {
            GenericRootEntity result;
            try {
                GenericRepository repository = genericMapper.mapToRepository(entity.getClass().getSimpleName());
                result = repository.create(entity);
            } catch (DataBaseRepositoryNotFoundException repositoryNotFoundException) {
                throw new DataBaseRepositoryNotFoundException(repositoryNotFoundException.getMessage(), repositoryNotFoundException);
            } catch (DataBaseGenericException dataBaseGenericException) {
                throw new DataBaseGenericException(dataBaseGenericException.getMessage(), dataBaseGenericException);
            } catch (Exception superCallException) {
                throw superCallException;
            }
            if (result == null) {
                throw new DataBaseGenericException("result is null");
            }
            return result;

    }

    /*
    public <T> T create(@NotNull T object, @NotNull Class<T> objectType) throws DataBaseGenericException {
            T result;
            try {
                GenericRepository repository = genericMapper.mapToRepository(objectType.getSimpleName());
                result = repository.create(object);
            } catch (DataBaseRepositoryNotFoundException repositoryNotFoundException) {
                throw new DataBaseRepositoryNotFoundException(repositoryNotFoundException.getMessage(), repositoryNotFoundException);
            } catch (DataBaseGenericException dataBaseGenericException) {
                throw new DataBaseGenericException(dataBaseGenericException.getMessage(), dataBaseGenericException);
            } catch (Exception superCallException) {
                throw superCallException;
            }
            if (result == null) {
                throw new DataBaseGenericException("result is null");
            }
            return result;

    }
    * */

}
