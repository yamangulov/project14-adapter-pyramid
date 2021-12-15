package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

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

    public MeterPointsByMeterParametersBatchWriter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @BeforeStep
    private void initData(StepExecution stepExecution) {
        this.exchange = stepExecution.getJobExecution().getJobParameters().getString("exchange");
        this.routingKey = stepExecution.getJobExecution().getJobParameters().getString("routingKey");
    }

    @Override
    public void write(List<? extends List<Reading>> items) throws Exception {
        LOGGER.info("Writing Reading on step1 into RabbitMQ");

        List<Reading> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        readings.forEach(reading -> {
            rabbitTemplate.convertAndSend(this.exchange, this.routingKey, reading);
        });

        LOGGER.info("End writing Reading on step1 into RabbitMQ");
    }
}
