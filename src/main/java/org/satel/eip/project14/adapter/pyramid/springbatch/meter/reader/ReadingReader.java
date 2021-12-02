package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.web.client.RestTemplate;

public class ReadingReader implements ItemReader<String> {
    public ReadingReader(String pyramidRestUrl, RestTemplate restTemplate) {
    }

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        return null;
    }
}
