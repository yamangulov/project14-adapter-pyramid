package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class MeterParametresWithStatusWriter implements ItemWriter<List<String>> {
    public MeterParametresWithStatusWriter(RabbitTemplate rabbitTemplate) {
    }

    @Override
    public void write(List<? extends List<String>> items) throws Exception {

    }
}
