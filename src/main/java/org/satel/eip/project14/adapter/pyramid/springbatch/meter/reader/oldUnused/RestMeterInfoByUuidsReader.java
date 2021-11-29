package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.oldUnused;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.AfterStep;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class RestMeterInfoByUuidsReader implements ItemReader<String> {

    static final Logger LOGGER = LoggerFactory.getLogger(RestMeterInfoByUuidsReader.class);

    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final ConcurrentHashMap<String, String> uuids;
    private boolean done;
    private String externalJobId;
    private List<String> jsonUuids;

    public RestMeterInfoByUuidsReader(String apiUrl, RestTemplate restTemplate, ConcurrentHashMap<String, String> uuids) {
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplate;
        this.uuids = uuids;
    }

    @BeforeStep
    private void setCurrentJobUuids(StepExecution stepExecution) {
        this.externalJobId = stepExecution.getJobExecution().getJobParameters().getString("externalJobId");
        this.jsonUuids = Arrays.stream(uuids.get(externalJobId).split(","))
                .map(s -> s.replaceAll("\"", ""))
                .collect(Collectors.toList());
        this.done = false;
    }

    @Override
    public String read() throws Exception {

        if (!this.done) {
            LOGGER.info("Reading the information from " + this.apiUrl);
            String requestJson = "[" +
                    jsonUuids.stream()
                            .map(s -> "\"" + s + "\"")
                            .collect(Collectors.joining(","))
                    + "]";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);
            String result = restTemplate.postForObject(this.apiUrl, entity, String.class);
            LOGGER.info("Got response");

            this.done = true;
            return result;

        } else {
            return null;

        }
    }

    @AfterStep
    private void clearUuids() {
        this.uuids.remove(externalJobId);
    }
}
