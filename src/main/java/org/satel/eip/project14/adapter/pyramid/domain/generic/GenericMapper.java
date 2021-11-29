package org.satel.eip.project14.adapter.pyramid.domain.generic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.DataBaseRepositoryNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@AllArgsConstructor
public class GenericMapper {

    private static final Map<String, GenericRepository> REPOSITORY_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Class> ENTITY_CLASS_MAP = new ConcurrentHashMap<>();

    private final List<GenericRepository> repositoryList;

    private final ObjectMapper objectMapper;

    @PostConstruct
    private void postConstruct() {
        try {
            log.info("GenericRepositoryMapper initiating start");
            Map<String, GenericRepository> entityToRepositoryMap = generateRepositoryMap();
            REPOSITORY_MAP.putAll(entityToRepositoryMap);
            Map<String, Class> entityClassMap = generateEntityClassMap();
            ENTITY_CLASS_MAP.putAll(entityClassMap);
            log.info("REPOSITORY_MAP:\t" + REPOSITORY_MAP);
            log.info("ENTITY_CLASS_MAP:\t" + ENTITY_CLASS_MAP);
            log.info("GenericRepositoryMapper initiating end");
        } catch (Exception e) {
            //todo shutdown event
            log.error(e.getMessage());
        }

    }


    private Map<String, GenericRepository> generateRepositoryMap() {
        Map<String, GenericRepository> repositoryMap = new ConcurrentHashMap<>();
        for (GenericRepository repository : repositoryList) {
            TargetEntity targetEntity = repository.getClass().getAnnotation(TargetEntity.class);
            repositoryMap.put(targetEntity.value().getSimpleName(), repository);
        }

        return repositoryMap;
    }

    @SuppressWarnings("rawtypes")
    private Map<String, Class> generateEntityClassMap() throws ClassNotFoundException {
        Map<String, Class> entityClassMap = new ConcurrentHashMap<>();

        for (GenericRepository repository : repositoryList) {
            TargetEntity targetEntity = repository.getClass().getAnnotation(TargetEntity.class);
            entityClassMap.put(targetEntity.value().getSimpleName(), Class.forName(targetEntity.value().getCanonicalName()));
        }
        return entityClassMap;
    }

    public GenericRepository mapToRepository(String targetEntity) throws DataBaseRepositoryNotFoundException {
        if (REPOSITORY_MAP.containsKey(targetEntity) && REPOSITORY_MAP.get(targetEntity) != null) {
            return REPOSITORY_MAP.get(targetEntity);
        } else {
            throw new DataBaseRepositoryNotFoundException();
        }
    }

    @SuppressWarnings("rawtypes")
    public Class mapToClass(String targetEntity) throws ClassNotFoundException {
        if (ENTITY_CLASS_MAP.containsKey(targetEntity) && ENTITY_CLASS_MAP.get(targetEntity) != null) {
            return ENTITY_CLASS_MAP.get(targetEntity);
        } else {
            throw new ClassNotFoundException("for targetEntity: " + targetEntity);
        }
    }


    @SuppressWarnings("unchecked")
    public GenericRootEntity mapToEntity(String entityType, String json) throws ClassNotFoundException, JsonProcessingException {
        return (GenericRootEntity) objectMapper.readValue(json, mapToClass(entityType));
    }

    /*
     @SuppressWarnings("unchecked")
    public Object mapToEntity(String entityType, String json) throws ClassNotFoundException, JsonProcessingException {
        return objectMapper.readValue(json, mapToClass(entityType));
    }
    * */

    /*public String returnBaseEntityType(String msg) throws JsonProcessingException {
        try {
            return objectMapper.readValue(msg, BaseEntity.class).getEntityType();
        } catch (Throwable e) {
            //todo что если сообщение не json (xml, byte) пустое
            return null;
        }
    }*/



}