package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.AccumulatorService;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.JobCompletionNotificationListener;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterEventsReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterPointsByMeterParametersBatchReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.request.CustomHttpComponentsClientHttpRequestFactory;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterEventsWriter;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterPointsByMeterParametersBatchWriter;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.satel.eip.project14.data.model.pyramid.Reading;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit.RabbitConstant.CHUNK_SIZE;
import static org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit.RabbitConstant.PYRAMID_REST_URI;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@RefreshScope
public class RestMeterBatchConfiguration {
    @Value(PYRAMID_REST_URI)
    private String pyramidRestUrl;

    @Value(CHUNK_SIZE)
    private int chunkSize;

    final JobBuilderFactory jobBuilderFactory;
    final StepBuilderFactory stepBuilderFactory;

    private final ObjectMapper objectMapper;
    private final AccumulatorService accumulatorService;

    @Autowired
    public RestMeterBatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, ObjectMapper objectMapper, AccumulatorService accumulatorService) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.objectMapper = objectMapper;
        this.accumulatorService = accumulatorService;
    }

    @Bean("restTemplate")
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("customRestTemplate")
    RestTemplate customRestTemplate() {
        return new RestTemplate(new CustomHttpComponentsClientHttpRequestFactory());
    }

    @Bean("commandParametersMap")
    public ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    @RefreshScope
    public Job getMeterJob(Step meterPointsByMeterParametersBatchStep, Step meterEventsStep, @Qualifier("rabbitTemplateSuccessCommand") RabbitTemplate rabbitTemplateSuccessCommand) {
        return jobBuilderFactory.get("pyramidJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobCompletionNotificationListener(rabbitTemplateSuccessCommand))
                .start(meterPointsByMeterParametersBatchStep)
                .next(meterEventsStep)
                .build();
    }

    //endpoint GET /meterpointsbymeterparametersbatch/{parameterguid}/{dtfrom}/{dtto}
    // + extra body in GET request with {meterguid} list with comma separator in it
    @Bean
    @RefreshScope
    public Step meterPointsByMeterParametersBatchStep(@Qualifier("customRestTemplate") RestTemplate customRestTemplate, @Qualifier("rabbitTemplateMeterReadings") RabbitTemplate rabbitTemplateMeterReadings, @Qualifier("rabbitTemplateConsolidations") RabbitTemplate rabbitTemplateConsolidations, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper, AccumulatorService accumulatorService) {
        return stepBuilderFactory.get("stepMeterPointsByMeterParametersBatchStep")
                .<List<Reading>, List<Reading>> chunk(chunkSize)
                .reader(new MeterPointsByMeterParametersBatchReader(pyramidRestUrl, customRestTemplate, commandParametersMap, objectMapper, accumulatorService))
                .writer(new MeterPointsByMeterParametersBatchWriter(rabbitTemplateMeterReadings, rabbitTemplateConsolidations, objectMapper, accumulatorService))
                .build();
    }

    //endpoint GET /meterevents/{meterguid}/{dtfrom}/{dtto}
    @Bean
    @RefreshScope
    public Step meterEventsStep(@Qualifier("restTemplate") RestTemplate restTemplate, @Qualifier("rabbitTemplateEvents") RabbitTemplate rabbitTemplateEvents, @Qualifier("rabbitTemplateConsolidations") RabbitTemplate rabbitTemplateConsolidations, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper, AccumulatorService accumulatorService) {
        return stepBuilderFactory.get("stepMeterEvents")
                .<List<EndDeviceEvent>, List<EndDeviceEvent>>chunk(chunkSize)
                .reader(new MeterEventsReader(pyramidRestUrl, restTemplate, commandParametersMap, objectMapper, accumulatorService))
                .writer(new MeterEventsWriter(rabbitTemplateEvents, rabbitTemplateConsolidations, objectMapper, accumulatorService))
                .build();
    }

}
