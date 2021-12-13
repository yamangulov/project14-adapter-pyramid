package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEventDetail;
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MeterEventsDetailReader implements ItemReader<Map<String, List<EndDeviceEvent>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsDetailReader.class);

    private final ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap;
    private final ConcurrentHashMap<String, Object> stepsResultsMap;
    private final String pyramidRestUrl;
    private final RestTemplate restTemplate;
    private String externalJobId;
    private boolean done;

    public MeterEventsDetailReader(String pyramidRestUrl, RestTemplate restTemplate, ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap, ConcurrentHashMap<String, Object> stepsResultsMap) {
        this.commandParametersMap = commandParametersMap;
        this.pyramidRestUrl = pyramidRestUrl;
        this.restTemplate = restTemplate;
        this.stepsResultsMap = stepsResultsMap;
    }

    @BeforeStep
    private void setCurrentJobGuids(StepExecution stepExecution) {
        this.externalJobId = stepExecution.getJobExecution().getJobParameters().getString("externalJobId");
        this.done = false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, List<EndDeviceEvent>> read() throws UnexpectedInputException, ParseException, NonTransientResourceException, Exception {
        if (!this.done) {
            LOGGER.info("Reading EndDeviceEventDetail for each meterevent from " + this.pyramidRestUrl);
            Map<String, List<EndDeviceEvent>> endDeviceEventsByMeterGuids = (Map<String, List<EndDeviceEvent>>) stepsResultsMap.get(externalJobId.concat("_EndDeviceEvent"));
            endDeviceEventsByMeterGuids.forEach((meterEvent, endDeviceEvents) -> {
                endDeviceEvents.forEach(endDeviceEvent -> {
                    // objectGuid = endDeviceEvent.getEventGuid();
                    // запрос GET /object/{objectGuid}  для получения по каждому ПУ
                    // для каждого из полученных для него EndDeviceEvent детального описания события
                    // EndDeviceEventDetail и обогащение им EndDeviceEvent
                    String request = pyramidRestUrl + "/" + endDeviceEvent.getEventGuid();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    EndDeviceEventDetail result = restTemplate.getForObject(request, EndDeviceEventDetail.class, entity);
                    endDeviceEvent.setDetails(result);
                });
            });
            LOGGER.info("End reading EndDeviceEventDetail for each meterevent from " + this.pyramidRestUrl);
            return endDeviceEventsByMeterGuids;
        } else {
            return null;
        }
    }
}
