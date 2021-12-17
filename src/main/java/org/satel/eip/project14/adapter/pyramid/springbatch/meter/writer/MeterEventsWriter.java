package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

public class MeterEventsWriter implements ItemWriter<List<EndDeviceEvent>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsWriter.class);

    private final RabbitTemplate rabbitTemplate;
    private String exchange;
    private String routingKey;
    private String meterReadingsQueue;
    private String defaultQueue;
    private final ObjectMapper objectMapper;

    public MeterEventsWriter(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @BeforeStep
    private void initData(StepExecution stepExecution) {
        this.exchange = stepExecution.getJobExecution().getJobParameters().getString("exchange");
        this.routingKey = stepExecution.getJobExecution().getJobParameters().getString("readingsRoutingKey");
        this.meterReadingsQueue = stepExecution.getJobExecution().getJobParameters().getString("meterReadingsQueue");
        this.defaultQueue = stepExecution.getJobExecution().getJobParameters().getString("defaultQueue");
    }

    @Override
    public void write(List<? extends List<EndDeviceEvent>> items) {
        LOGGER.info("Writing EndDeviceEvents on step2 into RabbitMQ");

        List<EndDeviceEvent> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        rabbitTemplate.setDefaultReceiveQueue(meterReadingsQueue);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                LOGGER.info("Error in MeterEventsWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
            }
            if (readingString != null) {
                rabbitTemplate.convertAndSend(this.exchange, this.routingKey, readingString);
            }
        });

        LOGGER.info("End writing EndDeviceEvents on step2 into RabbitMQ");
    }

}
