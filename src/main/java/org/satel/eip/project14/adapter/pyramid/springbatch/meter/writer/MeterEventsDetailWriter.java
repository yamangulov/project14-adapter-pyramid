package org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequest;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.satel.eip.project14.data.model.pyramid.MeterReading;
import org.satel.eip.project14.data.model.pyramid.Reading;
import org.satel.eip.project14.data.model.pyramid.wrapper.MeterReadingWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemWriter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MeterEventsDetailWriter implements ItemWriter<Map<String, Map<String, List<EndDeviceEvent>>>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MeterEventsDetailWriter.class);

    private final ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap;
    private final ConcurrentHashMap<String, Object> stepsResultsMap;
    private final RabbitTemplate rabbitTemplate;
    private String exchange;
    private String routingKey;

    public MeterEventsDetailWriter(ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap, RabbitTemplate rabbitTemplate, ConcurrentHashMap<String, Object> stepsResultsMap) {
        this.commandParametersMap = commandParametersMap;
        this.stepsResultsMap = stepsResultsMap;
        this.rabbitTemplate = rabbitTemplate;
    }

    @BeforeStep
    private void initData(StepExecution stepExecution) {
        this.exchange = stepExecution.getJobExecution().getJobParameters().getString("exchange");
        this.routingKey = stepExecution.getJobExecution().getJobParameters().getString("routingKey");
    }

    @Override
    @SuppressWarnings("unchecked")
    public void write(List<? extends Map<String, Map<String, List<EndDeviceEvent>>>> list) {

        LOGGER.info("Start constructing objects for response to RabbitMQ");

        String externalJobId = list.get(0).keySet().stream().findFirst().orElseThrow();

        // собираем из чанков endDeviceEventsByMeterGuids, уже обогащенные в MeterEventsDetailReader
        Map<String, List<EndDeviceEvent>> endDeviceEventsByMeterGuids = new ConcurrentHashMap<>();
        list.forEach(element -> {
            element.values().forEach(item -> {
                item.forEach((meterGuid, endDeviceEvents) -> {
                    if (endDeviceEventsByMeterGuids.containsKey(meterGuid)) {
                        endDeviceEventsByMeterGuids.get(meterGuid).addAll(endDeviceEvents);
                    } else {
                        endDeviceEventsByMeterGuids.put(meterGuid, endDeviceEvents);
                    }
                });
            });
        });

        // забираем из хранилища промежуточных результатов readingsByMeterGuidAndParmeterGuid
        Map<String, Map<String, List<Reading>>> readingsByMeterGuidAndParmeterGuid = (Map<String, Map<String, List<Reading>>>) stepsResultsMap.get(externalJobId.concat("_Reading"));
        // забираем из хранилища команд даты
        GetMeterRequest body = commandParametersMap
                .get(externalJobId).getCommandParameters().getBody();
        Instant dtFrom = body.getBeginDateTime();
        Instant dtTo = body.getEndDateTime();

        // Итоговый список объектов для обогащения и возврата в RabbitMQ
        List<MeterReadingWrapper> wrappedMeterReadings = new ArrayList<>();

        readingsByMeterGuidAndParmeterGuid.forEach((meterGuid, readingsByParameter) -> {
            List<EndDeviceEvent> endDeviceEvents = endDeviceEventsByMeterGuids.get(meterGuid);
            readingsByParameter.forEach((parameter, readings) -> {
                MeterReading meterReading = new MeterReading();
                meterReading.setMeterGuid(meterGuid);
                meterReading.setParameterGuid(parameter);
                meterReading.setMeterDtFrom(dtFrom);
                meterReading.setMeterDtTo(dtTo);
                meterReading.setMeterData(readings);
                meterReading.setEndDeviceEventsData(endDeviceEvents);
                MeterReadingWrapper meterReadingWrapper = new MeterReadingWrapper();
                meterReadingWrapper.setExternalJobId(UUID.fromString(externalJobId));
                meterReadingWrapper.setMeterReading(meterReading);
                wrappedMeterReadings.add(meterReadingWrapper);
            });
        });

        LOGGER.info("End constructing objects for response to RabbitMQ");

        LOGGER.info("Sending object into RabbitMQ");

        List<String> serializedObjects = new ArrayList<>();
        ObjectMapper mapper = new ObjectMapper();
        wrappedMeterReadings.forEach(wrappedReading -> {
            try {
                serializedObjects.add(mapper.writeValueAsString(wrappedReading));
            } catch (JsonProcessingException e) {
                LOGGER.error("Error on serializing object {}:\n {}", wrappedReading, e.getMessage());
            }
        });

        serializedObjects.forEach(jsonString -> {
            rabbitTemplate.convertAndSend(this.exchange, this.routingKey, jsonString);
        });

        LOGGER.info("Sending object into RabbitMQ");
    }
}
