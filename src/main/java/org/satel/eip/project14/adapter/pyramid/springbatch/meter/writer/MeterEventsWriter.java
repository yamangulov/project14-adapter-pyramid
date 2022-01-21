package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.DoubleAccumulator;

public class MeterEventsWriter implements ItemWriter<List<EndDeviceEvent>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsWriter.class);

    private final RabbitTemplate rabbitTemplate;
    private String exchange;
    private String eventsRoutingKey;
    private String eventsQueue;
    private String defaultQueue;
    private final ObjectMapper objectMapper;
    private String consolidationsQueue;
    private String consolidationsRoutingKey;
    private final Counter outCounter;
    private DoubleAccumulator outGaugeCounter;
    private final Gauge outGauge;

    public MeterEventsWriter(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, Counter outCounter, DoubleAccumulator outGaugeCounter, Gauge outGauge) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.outCounter = outCounter;
        this.outGaugeCounter = outGaugeCounter;
        this.outGauge = outGauge;
    }

    @BeforeStep
    private void initData(StepExecution stepExecution) {
        this.exchange = stepExecution.getJobExecution().getJobParameters().getString("exchange");
        this.eventsRoutingKey = stepExecution.getJobExecution().getJobParameters().getString("eventsRoutingKey");
        this.eventsQueue = stepExecution.getJobExecution().getJobParameters().getString("eventsQueue");
        this.defaultQueue = stepExecution.getJobExecution().getJobParameters().getString("defaultQueue");
        this.consolidationsRoutingKey = stepExecution.getJobExecution().getJobParameters().getString("consolidationsRoutingKey");
        this.consolidationsQueue = stepExecution.getJobExecution().getJobParameters().getString("consolidationsQueue");
    }

    @Override
    public void write(List<? extends List<EndDeviceEvent>> items) {
        LOGGER.info("Writing EndDeviceEvents on step2 into RabbitMQ");

        List<EndDeviceEvent> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        rabbitTemplate.setDefaultReceiveQueue(eventsQueue);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                LOGGER.info("Error in MeterEventsWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
            }
            if (readingString != null) {
                rabbitTemplate.convertAndSend(this.exchange, this.eventsRoutingKey, readingString);
            }
        });
        rabbitTemplate.setDefaultReceiveQueue(consolidationsQueue);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                LOGGER.info("Error in MeterEventsWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
            }
            if (readingString != null) {
                rabbitTemplate.convertAndSend(this.exchange, this.consolidationsRoutingKey, readingString);
                outCounter.increment();
                outGaugeCounter.accumulate(1.0);
                outGauge.measure();
            }
        });
        rabbitTemplate.setDefaultReceiveQueue(defaultQueue);

        LOGGER.info("End writing EndDeviceEvents on step2 into RabbitMQ");
    }

}
