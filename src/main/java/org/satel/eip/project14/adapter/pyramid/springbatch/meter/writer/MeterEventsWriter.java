package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

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

    public MeterEventsWriter(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @BeforeStep
    private void initData(StepExecution stepExecution) {
        this.exchange = stepExecution.getJobExecution().getJobParameters().getString("exchange");
        this.routingKey = stepExecution.getJobExecution().getJobParameters().getString("routingKey");
    }

    @Override
    public void write(List<? extends List<EndDeviceEvent>> items) {
        LOGGER.info("Writing EndDeviceEvents on step2 into RabbitMQ");

        List<EndDeviceEvent> readings = new ArrayList<>();
        items.forEach(readings::addAll);
        readings.forEach(reading -> {
            rabbitTemplate.convertAndSend(this.exchange, this.routingKey, reading);
        });

        LOGGER.info("End writing EndDeviceEvents on step2 into RabbitMQ");
    }

}
