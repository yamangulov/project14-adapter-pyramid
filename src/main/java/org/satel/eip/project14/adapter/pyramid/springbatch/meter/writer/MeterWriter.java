package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class MeterWriter implements ItemWriter<String> {
    public MeterWriter(RabbitTemplate rabbitTemplate) {
    }

    @Override
    public void write(List<? extends String> list) throws Exception {

    }
}
