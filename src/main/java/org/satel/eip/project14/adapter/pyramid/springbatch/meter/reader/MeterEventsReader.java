package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.RestRequestType;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.satel.eip.project14.data.model.pyramid.wrapper.StringRootDataWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MeterEventsReader implements ItemReader<List<EndDeviceEvent>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsReader.class);

    private final ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap;
    private final String pyramidRestUrl;
    private final RestTemplate restTemplate;
    private List<String> arrayOfGuids = new ArrayList<>();
    private Instant dtFrom;
    private Instant dtTo;
    private boolean done;
    private final ObjectMapper objectMapper;

    public MeterEventsReader(String pyramidRestUrl, RestTemplate restTemplate, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper) {
        this.commandParametersMap = commandParametersMap;
        this.pyramidRestUrl = pyramidRestUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @BeforeStep
    private void setCurrentJobGuids(StepExecution stepExecution) {
        String externalJobId = stepExecution.getJobExecution().getJobParameters().getString("externalJobId");
        GetMeterRequestCommand command = (GetMeterRequestCommand) commandParametersMap
                .get(externalJobId).getCommandParameters();
        GetMeterRequest body = command.getBody();
        this.arrayOfGuids = body.getArrayOfGuids().getGuid();
        this.dtFrom = body.getBeginDateTime();
        this.dtTo = body.getEndDateTime();
        this.done = false;
    }

    @Override
    public List<EndDeviceEvent> read() throws UnexpectedInputException, ParseException, NonTransientResourceException {

        if (!this.done) {
            LOGGER.info("Reading the information of meterevents from " + this.pyramidRestUrl);

            Map<String, String> requestsByMeterGuids = new ConcurrentHashMap<>();
            this.arrayOfGuids.forEach(meterGuid -> {
                StringBuilder builder = new StringBuilder(pyramidRestUrl);
                builder.append(RestRequestType.METEREVENTS.getRootDir());
                builder.append("/");
                builder.append(meterGuid);
                builder.append("/");
                builder.append(dtFrom);
                builder.append("/");
                builder.append(dtTo);
                requestsByMeterGuids.put(meterGuid, builder.toString());
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            List<EndDeviceEvent> results = new ArrayList<>();
            // для каждого ПУ meterguid собственный запрос в рест апи
            requestsByMeterGuids.forEach((meterGuid, request) -> {
                String result = restTemplate.getForEntity(request, String.class, entity).toString();
                String resultInner = objectMapper.convertValue(result, StringRootDataWrapper.class).getJsonString();
                try {
                    List<EndDeviceEvent> resultList = Arrays.asList(objectMapper.readValue(resultInner, EndDeviceEvent[].class));
                    resultList.forEach(reading -> {
                        reading.setReceivedDate(Instant.now());
                        reading.setMeterGuid(UUID.fromString(meterGuid));
                    });
                    results.addAll(resultList);
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error on mapping of received data into EndDeviceEvent objects\n {}", e.getMessage());
                }
            });

            this.done = true;
            LOGGER.info("End reading the information of meterevents from " + this.pyramidRestUrl);
            return results;
        } else {
            return null;
        }

    }
}
