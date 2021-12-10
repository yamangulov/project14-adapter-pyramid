package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import org.satel.eip.project14.data.model.pyramid.Reading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MeterParametresWithStatusWriter implements ItemWriter<Map<String, Map<String, Map<String, List<Reading>>>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterParametresWithStatusWriter.class);
    private final ConcurrentHashMap<String, Object> stepsResultsMap;

    public MeterParametresWithStatusWriter(ConcurrentHashMap<String, Object> stepsResultsMap) {
        this.stepsResultsMap = stepsResultsMap;
    }

    @Override
    public void write(List<? extends Map<String, Map<String, Map<String, List<Reading>>>>> items) {
        LOGGER.info("Writing Reading on step1 to map for saving");

        String key = items.get(0).keySet().stream().findFirst().orElseThrow();
        Map<String, Map<String, List<Reading>>> value = new ConcurrentHashMap<>();
        items.forEach(item -> {
            item.values().forEach(levelOneValue -> {
                levelOneValue.forEach((meterguid, levelTwoValue) -> {
                    levelTwoValue.forEach((parameter, readings) -> {
                        if (value.containsKey(meterguid)) {
                            if (value.get(meterguid).containsKey(parameter)) {
                                value.get(meterguid).get(parameter).addAll(readings);
                            } else {
                                value.get(meterguid).put(parameter, readings);
                            }
                        } else {
                            Map<String, List<Reading>> readingsByParams = new ConcurrentHashMap<>();
                            readingsByParams.put(parameter, readings);
                            value.put(meterguid, readingsByParams);
                        }
                    });
                });
            });
        });
        stepsResultsMap.put(key, value);
        LOGGER.info("End writing Reading on step1 to map for saving");
    }
}
