package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MeterEventsWriter implements ItemWriter<Map<String, Map<String, List<EndDeviceEvent>>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsWriter.class);
    private final ConcurrentHashMap<String, Object> stepsResultsMap;

    public MeterEventsWriter(RabbitTemplate rabbitTemplate, ConcurrentHashMap<String, Object> stepsResultsMap) {
        this.stepsResultsMap = stepsResultsMap;
    }

    @Override
    public void write(List<? extends Map<String, Map<String, List<EndDeviceEvent>>>> items) {
        LOGGER.info("Writing EndDeviceEvents on step2 to map for saving");
        String key = items.get(0).keySet().stream().findFirst().orElseThrow();
        Map<String, List<EndDeviceEvent>> value = new ConcurrentHashMap<>();
        items.forEach(item -> {
            item.values().forEach(innerValue -> {
                innerValue.forEach((meterguid, endDeviceEvents) -> {
                    if (value.containsKey(meterguid)) {
                        value.get(meterguid).addAll(endDeviceEvents);
                    } else {
                        value.put(meterguid, endDeviceEvents);
                    }
                });
            });
        });
        stepsResultsMap.put(key, value);
        LOGGER.info("End writing EndDeviceEvents on step2 to map for saving");
    }

}
