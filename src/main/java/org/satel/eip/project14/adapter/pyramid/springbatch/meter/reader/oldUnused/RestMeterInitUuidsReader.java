package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.oldUnused;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class RestMeterInitUuidsReader implements ItemReader<String> {

    static final Logger LOGGER = LoggerFactory.getLogger(RestMeterInitUuidsReader.class);

    private final String apiUrl;
    private final RestTemplate restTemplate;

    private int nextObjectIndex;
    private List<String> objectData;

    public RestMeterInitUuidsReader(String apiUrl, RestTemplate restTemplate) {
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplate;
    }

    @BeforeStep
    private void initExecution() {
        this.nextObjectIndex = 0;
        this.objectData = null;
    }

    @Override
    public String read() throws Exception {
        if (objectDataIsNotInitialized()) {
            LOGGER.info("Reading meters");
            String response = this.restTemplate.getForEntity(this.apiUrl, String.class).getBody();

            ObjectMapper jsonObjectMapper = new ObjectMapper();
            ObjectNode node = jsonObjectMapper.readValue(response, ObjectNode.class);

            objectData = Arrays.asList(
                    jsonObjectMapper.convertValue(
                            node.get("meters"),
                            String[].class
                    )
            );
        }

        return currentJobStillNotFinished() ? objectData.get(nextObjectIndex++) : null;

    }

    @AfterStep
    private void clearData() {
        this.objectData = null;
    }

    private boolean objectDataIsNotInitialized() {
        return this.objectData == null;
    }

    private boolean currentJobStillNotFinished() {
        return nextObjectIndex < objectData.size();
    }

}

