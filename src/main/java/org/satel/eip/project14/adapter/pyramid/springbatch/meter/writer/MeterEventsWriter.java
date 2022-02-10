package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.DoubleAccumulator;

public class MeterEventsWriter implements ItemWriter<List<EndDeviceEvent>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsWriter.class);

    private final RabbitTemplate rabbitTemplateEvents;
    private final RabbitTemplate rabbitTemplateConsolidations;
    private final ObjectMapper objectMapper;
    private final Counter outCounter;
    private DoubleAccumulator outGaugeCounter;
    private final Gauge outGauge;

    public MeterEventsWriter(RabbitTemplate rabbitTemplateEvents, RabbitTemplate rabbitTemplateConsolidations, ObjectMapper objectMapper, Counter outCounter, DoubleAccumulator outGaugeCounter, Gauge outGauge) {
        this.rabbitTemplateEvents = rabbitTemplateEvents;
        this.rabbitTemplateConsolidations = rabbitTemplateConsolidations;
        this.objectMapper = objectMapper;
        this.outCounter = outCounter;
        this.outGaugeCounter = outGaugeCounter;
        this.outGauge = outGauge;
    }

    @Override
    public void write(List<? extends List<EndDeviceEvent>> items) {
        LOGGER.info("Writing EndDeviceEvents on step2 into RabbitMQ");

        List<EndDeviceEvent> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                LOGGER.info("Error in MeterEventsWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
            }
            if (readingString != null) {
                rabbitTemplateEvents.convertAndSend(readingString);
            }
        });

        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                LOGGER.info("Error in MeterEventsWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
            }
            if (readingString != null) {
                rabbitTemplateConsolidations.convertAndSend(readingString);
                outCounter.increment();
                outGaugeCounter.accumulate(1.0);
                outGauge.measure();
            }
        });

        LOGGER.info("End writing EndDeviceEvents on step2 into RabbitMQ");
    }

}
