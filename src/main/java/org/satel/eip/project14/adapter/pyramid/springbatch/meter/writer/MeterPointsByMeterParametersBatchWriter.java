package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.AccumulatorService;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity.AvailableMetrics;
import org.satel.eip.project14.data.model.pyramid.Reading;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class MeterPointsByMeterParametersBatchWriter implements ItemWriter<List<Reading>> {

    private final RabbitTemplate rabbitTemplateMeterReadings;
    private final RabbitTemplate rabbitTemplateConsolidations;
    private final ObjectMapper objectMapper;
    private final AccumulatorService accumulatorService;

    public MeterPointsByMeterParametersBatchWriter(RabbitTemplate rabbitTemplateMeterReadings, RabbitTemplate rabbitTemplateConsolidations, ObjectMapper objectMapper, AccumulatorService accumulatorService) {
        this.rabbitTemplateMeterReadings = rabbitTemplateMeterReadings;
        this.rabbitTemplateConsolidations = rabbitTemplateConsolidations;
        this.objectMapper = objectMapper;
        this.accumulatorService = accumulatorService;
    }

    @Override
    public void write(List<? extends List<Reading>> items) throws Exception {
        log.info("Writing Reading on step1 into RabbitMQ");

        List<Reading> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                log.info("Error in MeterPointsByMeterParametersBatchWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
                accumulatorService.increment(accumulatorService.getChannel("batchJob"), AvailableMetrics.BATCH_JOB_METER_POINTS_MAPPING_ERROR);
            }
            if (readingString != null) {
                rabbitTemplateMeterReadings.convertAndSend(readingString);
                rabbitTemplateConsolidations.convertAndSend(readingString);
                accumulatorService.increment(accumulatorService.getChannel("batchJob"), AvailableMetrics.BATCH_JOB_METER_POINTS_RETURNED_TOTAL);
            }
        });

        log.info("End writing Reading on step1 into RabbitMQ");
    }
}
