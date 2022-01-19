package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import org.satel.eip.project14.adapter.pyramid.wrapper.DoubleWrapper;
import org.satel.eip.project14.data.model.pyramid.Reading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

import java.util.ArrayList;
import java.util.List;

public class MeterPointsByMeterParametersBatchWriter implements ItemWriter<List<Reading>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterPointsByMeterParametersBatchWriter.class);

    private final RabbitTemplate rabbitTemplate;
    private String exchange;
    private String routingKey;
    private String meterReadingsQueue;
    private String defaultQueue;
    private final ObjectMapper objectMapper;
    private String consolidationsQueue;
    private String consolidationsRoutingKey;
    private final Counter outCounter;
    private DoubleWrapper outGaugeCounter;
    private final Gauge outGauge;

    public MeterPointsByMeterParametersBatchWriter(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper, Counter outCounter, DoubleWrapper outGaugeCounter, Gauge outGauge) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
        this.outCounter = outCounter;
        this.outGaugeCounter = outGaugeCounter;
        this.outGauge = outGauge;
    }

    @BeforeStep
    private void initData(StepExecution stepExecution) {
        this.exchange = stepExecution.getJobExecution().getJobParameters().getString("exchange");
        this.routingKey = stepExecution.getJobExecution().getJobParameters().getString("readingsRoutingKey");
        this.consolidationsRoutingKey = stepExecution.getJobExecution().getJobParameters().getString("consolidationsRoutingKey");
        this.meterReadingsQueue = stepExecution.getJobExecution().getJobParameters().getString("meterReadingsQueue");
        this.consolidationsQueue = stepExecution.getJobExecution().getJobParameters().getString("consolidationsQueue");
        this.defaultQueue = stepExecution.getJobExecution().getJobParameters().getString("defaultQueue");
    }

    @Override
    public void write(List<? extends List<Reading>> items) throws Exception {
        LOGGER.info("Writing Reading on step1 into RabbitMQ");

        List<Reading> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        rabbitTemplate.setDefaultReceiveQueue(meterReadingsQueue);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                LOGGER.info("Error in MeterPointsByMeterParametersBatchWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
            }
            if (readingString != null) {
                rabbitTemplate.convertAndSend(this.exchange, this.routingKey, readingString);
            }
        });
        rabbitTemplate.setDefaultReceiveQueue(consolidationsQueue);
        readings.forEach(reading -> {
            String readingString = null;
            try {
                readingString = objectMapper.writeValueAsString(reading);
            } catch (JsonProcessingException e) {
                LOGGER.info("Error in MeterPointsByMeterParametersBatchWriter on mapping reading {} into String:\n{}",
                        reading, e.getMessage());
            }
            if (readingString != null) {
                rabbitTemplate.convertAndSend(this.exchange, this.consolidationsRoutingKey, readingString);
                outCounter.increment();
                outGaugeCounter.setValue(outGaugeCounter.getValue() + 1.0);
                outGauge.measure();
            }
        });
        rabbitTemplate.setDefaultReceiveQueue(defaultQueue);

        LOGGER.info("End writing Reading on step1 into RabbitMQ");
    }
}
