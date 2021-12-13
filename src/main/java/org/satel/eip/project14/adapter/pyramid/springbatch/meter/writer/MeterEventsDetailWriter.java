package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;

public class MeterEventsDetailWriter implements ItemWriter<Map<String, List<EndDeviceEvent>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsDetailWriter.class);
    public MeterEventsDetailWriter(RabbitTemplate rabbitTemplate) {
    }

    @Override
    public void write(List<? extends Map<String, List<EndDeviceEvent>>> list) throws Exception {
        LOGGER.info("Start constructing objects for response to RabbitMQ");

        LOGGER.info("End constructing objects for response to RabbitMQ");
    }
}
