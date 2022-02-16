package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.AccumulatorService;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity.AvailableMetrics;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MeterEventsWriter implements ItemWriter<List<EndDeviceEvent>> {

    private final RabbitTemplate rabbitTemplateEvents;
    private final RabbitTemplate rabbitTemplateConsolidations;
    private final ObjectMapper objectMapper;
    private final AccumulatorService accumulatorService;

    public MeterEventsWriter(RabbitTemplate rabbitTemplateEvents, RabbitTemplate rabbitTemplateConsolidations, ObjectMapper objectMapper, AccumulatorService accumulatorService) {
        this.rabbitTemplateEvents = rabbitTemplateEvents;
        this.rabbitTemplateConsolidations = rabbitTemplateConsolidations;
        this.objectMapper = objectMapper;
        this.accumulatorService = accumulatorService;
    }

    @Override
    public void write(List<? extends List<EndDeviceEvent>> items) {
        log.info("Writing EndDeviceEvents on step2 into RabbitMQ");

        List<EndDeviceEvent> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                log.info("Error in MeterEventsWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
                accumulatorService.increment(accumulatorService.getChannel("batchJob"), AvailableMetrics.BATCH_JOB_END_DEVICE_EVENTS_MAPPING_ERROR);
            }
            if (readingString != null) {
                rabbitTemplateEvents.convertAndSend(readingString);
                rabbitTemplateConsolidations.convertAndSend(readingString);
                accumulatorService.increment(accumulatorService.getChannel("batchJob"), AvailableMetrics.BATCH_JOB_END_DEVICE_EVENTS_RETURNED_TOTAL);
            }
        });

        log.info("End writing EndDeviceEvents on step2 into RabbitMQ");
    }

}
