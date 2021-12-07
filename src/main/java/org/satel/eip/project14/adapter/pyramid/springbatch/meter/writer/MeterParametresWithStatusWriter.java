package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.data.model.pyramid.MeterReading;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.item.ItemWriter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MeterParametresWithStatusWriter implements ItemWriter<Map<String, List<String>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterParametresWithStatusWriter.class);
    private final RabbitTemplate rabbitTemplate;
    private final ConcurrentHashMap<String, Map<Integer, Object>> stepsResultsMap;
    private final ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap;

    public MeterParametresWithStatusWriter(RabbitTemplate rabbitTemplate, ConcurrentHashMap<String, Map<Integer, Object>> stepsResultsMap, ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap) {
        this.rabbitTemplate = rabbitTemplate;
        this.stepsResultsMap = stepsResultsMap;
        this.commandParametersMap = commandParametersMap;
    }

    @Override
    public void write(List<? extends Map<String, List<String>>> items) throws Exception {
        LOGGER.info("Writing MeterReading on step1 to map for saving");
        String key = items.get(0).keySet().stream().findFirst().orElseThrow().concat("_step1");
        // здесь мы пока еще не посылаем объекты в rabbit, а сохраняем его для последующего обогащения
        Map<Integer, Object> map = new ConcurrentHashMap<>();
        for (List<String> strings : items.get(0).values()) {
            int k = 0;
            for (String o : strings) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                MeterReading meterReading = mapper.convertValue(o, MeterReading.class);
                map.put(k, meterReading);
                k++;
            }
        }
        stepsResultsMap.put(key, map);

        LOGGER.info("End writing MeterReading on step1 to map for saving");

    }
}
