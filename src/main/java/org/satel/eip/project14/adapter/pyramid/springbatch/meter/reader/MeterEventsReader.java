package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.RestRequestType;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterParametresWithStatusWriter;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.satel.eip.project14.data.model.pyramid.MeterReading;
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

public class MeterEventsReader implements ItemReader<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsReader.class);

    private final ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap;
    private final String pyramidRestUrl;
    private final RestTemplate restTemplate;
    private List<String> arrayOfGuids = new ArrayList<>();
    private Instant dtFrom;
    private Instant dtTo;
    private boolean done;
    private String externalJobId;

    public MeterEventsReader(String pyramidRestUrl, RestTemplate restTemplate, ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap) {
        this.commandParametersMap = commandParametersMap;
        this.pyramidRestUrl = pyramidRestUrl;
        this.restTemplate = restTemplate;
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
    public String read() throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {

        Map<String, String> requests = new ConcurrentHashMap<>();
        this.arrayOfGuids.forEach(meterguid -> {
            StringBuilder builder = new StringBuilder(pyramidRestUrl);
            builder.append(RestRequestType.METEREVENTS);
            builder.append("/");
            builder.append(meterguid);
            builder.append("/");
            builder.append(dtFrom);
            builder.append("/");
            builder.append(dtTo);
            requests.put(meterguid, builder.toString());
        });

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        Map<String, List<EndDeviceEvent>> results = new ConcurrentHashMap<>();
        // для каждого ПУ meterguid собственный запрос в рест апи
        requests.forEach((meterguid, request) -> {
            String result = restTemplate.getForEntity(request, String.class).toString();
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            String resultInner = mapper.convertValue(result, StringRootDataWrapper.class).getJsonString();
            List<EndDeviceEvent> resultList = null;
            try {
                resultList = Arrays.asList(mapper.readValue(resultInner, EndDeviceEvent[].class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            results.put(meterguid, resultList);
        });

        //TODO сделать второй запрос в рест апи для получения по каждому ПУ для каждого из полученных для него
        // EndDeviceEvent детальное описание события GET /object/{objectGuid} и обогатить их полученными
        // EndDeviceEventDetail, затем вернуть обогащенные объекты из ридера
        return null;
    }

    @AfterStep
    private void clearGuids() {
        this.arrayOfGuids = new ArrayList<>();
    }
}
