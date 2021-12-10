package org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader;

import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.web.client.RestTemplate;

public class MeterEventsDetailReader implements ItemReader<String> {
    public MeterEventsDetailReader(String pyramidRestUrl, RestTemplate restTemplate) {
    }

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        //TODO сделать второй запрос в рест апи для получения по каждому ПУ для каждого из полученных для него
        // EndDeviceEvent детальное описание события GET /object/{objectGuid} и обогатить их полученными
        // EndDeviceEventDetail, затем вернуть обогащенные объекты
        return null;
    }
}
