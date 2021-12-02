package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;

public class ReadingWriter implements ItemWriter<String> {
    public ReadingWriter(RabbitTemplate rabbitTemplate) {
    }

    @Override
    public void write(List<? extends String> list) throws Exception {

    }
}
