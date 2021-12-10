package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.RestRequestType;
import org.satel.eip.project14.data.model.pyramid.MeterParameter;
import org.satel.eip.project14.data.model.pyramid.Reading;
import org.satel.eip.project14.data.model.pyramid.wrapper.StringRootDataWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
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
import java.util.concurrent.ConcurrentHashMap;

public class MeterParametresWithStatusReader implements ItemReader<Map<String, Map<String, Map<String, List<Reading>>>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterParametresWithStatusReader.class);

    private final String pyramidRestUrl;
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap;
    private List<String> arrayOfGuids = new ArrayList<>();
    private Instant dtFrom;
    private Instant dtTo;
    private boolean done;
    private String externalJobId;

    public MeterParametresWithStatusReader(String pyramidRestUrl, RestTemplate restTemplate, ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap) {
        this.pyramidRestUrl = pyramidRestUrl;
        this.restTemplate = restTemplate;
        this.commandParametersMap = commandParametersMap;
    }

    @BeforeStep
    private void setCurrentJobGuids(StepExecution stepExecution) {
        this.externalJobId = stepExecution.getJobExecution().getJobParameters().getString("externalJobId");
        GetMeterRequest body = commandParametersMap
                .get(externalJobId).getCommandParameters().getBody();
        this.arrayOfGuids = body.getArrayOfGuids().getGuids();
        this.dtFrom = body.getBeginDateTime();
        this.dtTo = body.getEndDateTime();
        this.done = false;
    }


    @Override
    public Map<String, Map<String, Map<String, List<Reading>>>> read() throws UnexpectedInputException, ParseException, NonTransientResourceException {

        if (!this.done) {
            LOGGER.info("Reading the information of meterparameterswithstatus from " + this.pyramidRestUrl);

            Map<String, Map<String, String>> requests = new ConcurrentHashMap<>();
            this.arrayOfGuids.forEach(meterguid -> Arrays.stream(MeterParameter.values()).forEach(meterParameter -> {
                String meterParameterGuid = meterParameter.getParameterGuid();
                StringBuilder builder = new StringBuilder(pyramidRestUrl);
                builder.append(RestRequestType.METERPARAMETERSWITHSTATUS);
                builder.append("/");
                builder.append(meterguid);
                builder.append("/");
                builder.append(meterParameterGuid);
                builder.append("/");
                builder.append(dtFrom);
                builder.append("/");
                builder.append(dtTo);
                String reqString = builder.toString();
                if (requests.get(meterguid) != null) {
                    requests.get(meterguid).put(meterParameterGuid, reqString);
                } else {
                    Map<String, String> meterRequestByParameter = new ConcurrentHashMap<>();
                    meterRequestByParameter.put(meterParameterGuid, reqString);
                    requests.put(meterguid, meterRequestByParameter);
                }
            }));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            Map<String, Map<String, List<Reading>>> results = new ConcurrentHashMap<>();
            requests.forEach((meterguid, parametersMap) -> {
                Map<String, List<Reading>> readingsByParams = new ConcurrentHashMap<>();
                parametersMap.forEach((parameter, request) -> {
                    String result = restTemplate.getForEntity(request, String.class, entity).toString();
                    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                    String resultInner = mapper.convertValue(result, StringRootDataWrapper.class).getJsonString();
                    List<Reading> readings = new ArrayList<>();
                    try {
                        readings = Arrays.asList(mapper.readValue(resultInner, Reading[].class));
                    } catch (JsonProcessingException e) {
                        LOGGER.error("Error on mapping of received data into Reading objects\n {}", e.getMessage());
                    }
                    readingsByParams.put(parameter, readings);
                });
                results.put(meterguid, readingsByParams);
            });

            Map<String, Map<String, Map<String, List<Reading>>>> wrappedResults = new ConcurrentHashMap<>();
            //"{externalJobId}_Reading" пишем в ключ мапы, чтобы различать по ключу в следующих шагах
            // значения только для своего Job и только для своего step
            wrappedResults.put(externalJobId.concat("_Reading"), results);
            this.done = true;
            LOGGER.info("End reading the information of meterparameterswithstatus from " + this.pyramidRestUrl);
            return wrappedResults;
        } else {
            return null;
        }
    }

    @AfterStep
    private void clearGuids() {
        this.arrayOfGuids = new ArrayList<>();
    }
}
