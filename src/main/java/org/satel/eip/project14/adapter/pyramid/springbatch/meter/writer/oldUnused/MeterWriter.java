package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.oldUnused;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class MeterWriter implements ItemWriter<List<String>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MeterWriter.class);
    private final RabbitTemplate rabbitTemplate;

    public MeterWriter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void write(List<? extends List<String>> list) throws Exception {
        LOGGER.info("Writing to Rabbit");
        for (List<String> innerList : list) {
            for (String o : innerList) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                String prettyJson = mapper.writeValueAsString(o);
                rabbitTemplate.convertAndSend("PROJECT14", "METERS", prettyJson);
            }
        }

        LOGGER.info("End writing to Rabbit");
    }
}

