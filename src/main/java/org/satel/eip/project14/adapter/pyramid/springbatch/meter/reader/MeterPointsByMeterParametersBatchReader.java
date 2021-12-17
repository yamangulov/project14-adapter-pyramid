package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.RestRequestType;
import org.satel.eip.project14.data.model.pyramid.MeterParameter;
import org.satel.eip.project14.data.model.pyramid.Reading;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MeterPointsByMeterParametersBatchReader implements ItemReader<List<Reading>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterPointsByMeterParametersBatchReader.class);

    private final String pyramidRestUrl;
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap;
    private String meterGuids;
    private Instant dtFrom;
    private Instant dtTo;
    private boolean done;
    private final ObjectMapper objectMapper;

    public MeterPointsByMeterParametersBatchReader(String pyramidRestUrl, RestTemplate restTemplate, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper) {
        this.pyramidRestUrl = pyramidRestUrl;
        this.restTemplate = restTemplate;
        this.commandParametersMap = commandParametersMap;
        this.objectMapper = objectMapper;
    }

    @BeforeStep
    private void setCurrentJobGuids(StepExecution stepExecution) {
        String externalJobId = stepExecution.getJobExecution().getJobParameters().getString("externalJobId");
        GetMeterRequestCommand command = (GetMeterRequestCommand) commandParametersMap
                .get(externalJobId).getCommandParameters();
        GetMeterRequest body = command.getBody();
        this.meterGuids = String.join(",", body.getArrayOfGuids().getGuid());
        this.dtFrom = body.getBeginDateTime();
        this.dtTo = body.getEndDateTime();
        this.done = false;
    }

    @Override
    public List<Reading> read() throws UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!this.done) {
            LOGGER.info("Reading the information of meterpointsbymeterparametersbatch from " + this.pyramidRestUrl);

            Map<String, String> requestsByParameters = new ConcurrentHashMap<>();
            Arrays.stream((MeterParameter.values())).forEach(meterParameter -> {
                String meterParameterGuid = meterParameter.getParameterGuid();
                StringBuilder builder = new StringBuilder(pyramidRestUrl);
                builder.append(RestRequestType.METERPOINTSBYMETERPARAMETERSBATCH.getRootDir());
                builder.append("/");
                builder.append(meterParameterGuid);
                builder.append("/");
                builder.append(dtFrom);
                builder.append("/");
                builder.append(dtTo);
                String reqString = builder.toString();
                requestsByParameters.put(meterParameterGuid, reqString);
            });

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            //meterGuids from commandParametersMap pass as body into custom GET request
            HttpEntity<String> entity = new HttpEntity<>(meterGuids, headers);

            List<Reading> results = new ArrayList<>();
            //default String GET here doesn't work with body, only POST. Способ применения кастомного RestTemplate:
            // String responseBody = restTemplate.exchange(endpoint, HttpMethod.GET, new HttpEntity<>(requestBody), String.class).getBody();
            requestsByParameters.forEach((meterParameterGuid, reqString) -> {
                String resultInner = restTemplate.exchange(reqString, HttpMethod.GET, entity, String.class).getBody();
                try {
                    List<Reading> resultList = Arrays.asList(objectMapper.readValue(resultInner, Reading[].class));
                    resultList.forEach(reading -> {
                        reading.setReceivedDate(Instant.now());
                        reading.setParameterGuid(UUID.fromString(meterParameterGuid));
                    });
                    if (!resultList.isEmpty()) {
                        results.addAll(resultList);
                    }
                } catch (JsonProcessingException e) {
                    LOGGER.error("Error on mapping of received data into Reading objects\n {}", e.getMessage());
                }
            });
            this.done = true;
            LOGGER.info("End reading the information of meterpointsbymeterparametersbatch from " + this.pyramidRestUrl);
            return results;
        } else {
            return null;
        }
    }
}
