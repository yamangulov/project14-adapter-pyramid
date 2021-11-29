package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.oldUnused;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;


public class MeterUuidsWriter implements ItemWriter<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterUuidsWriter.class);
    private final RabbitTemplate rabbitTemplate;

    public MeterUuidsWriter (RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void write(List<? extends String> items) throws Exception {
        LOGGER.info("Received the information of {}", items.getClass());
        LOGGER.info("Writing to Rabbit");

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String itemsJson = mapper.writeValueAsString(items);
        rabbitTemplate.convertAndSend("PROJECT14", "METER_UUIDS", itemsJson);

        LOGGER.info("End writing to Rabbit");
    }

}
