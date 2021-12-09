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
    private final ConcurrentHashMap<String, Object> stepsResultsMap;

    public MeterParametresWithStatusWriter(ConcurrentHashMap<String, Object> stepsResultsMap) {
        this.stepsResultsMap = stepsResultsMap;
    }

    @Override
    public void write(List<? extends Map<String, List<String>>> items) {
        LOGGER.info("Writing MeterReading on step1 to map for saving");
        //"{externalJobId}_MeterReading" пишем в ключ мапы, чтобы различать по ключу в следующих шагах
        // значения только для своего Job и только для своего step
        String key = items.get(0).keySet().stream().findFirst().orElseThrow().concat("_MeterReading");
        // здесь мы пока еще не посылаем объекты в rabbit, а сохраняем их для последующего обогащения
        for (List<String> strings : items.get(0).values()) {
            for (String o : strings) {
                ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
                MeterReading meterReading = mapper.convertValue(o, MeterReading.class);
                stepsResultsMap.put(key, meterReading);
            }
        }
        LOGGER.info("End writing MeterReading on step1 to map for saving");
    }
}
