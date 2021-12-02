package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.web.client.RestTemplate;

public class MeterParametresWithStatusReader implements ItemReader<String> {
    private final String pyramidRestUrl;
    private final RestTemplate restTemplate;

    public MeterParametresWithStatusReader(String pyramidRestUrl, RestTemplate restTemplate) {
        this.pyramidRestUrl = pyramidRestUrl;
        this.restTemplate = restTemplate;
    }


    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        return null;
    }
}
