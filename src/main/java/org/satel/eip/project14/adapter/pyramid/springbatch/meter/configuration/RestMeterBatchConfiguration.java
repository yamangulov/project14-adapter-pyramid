package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.JobCompletionNotificationListener;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterEventsReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterPointsByMeterParametersBatchReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.request.CustomHttpComponentsClientHttpRequestFactory;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterEventsWriter;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterPointsByMeterParametersBatchWriter;
import org.satel.eip.project14.data.model.pyramid.EndDeviceEvent;
import org.satel.eip.project14.data.model.pyramid.Reading;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
@EnableBatchProcessing
public class RestMeterBatchConfiguration {
    @Value("${pyramid.rest.url}")
    private String pyramidRestUrl;

    @Value("${commands.GetMeter.limit:20}")
    private int chunkSize;

    final JobBuilderFactory jobBuilderFactory;
    final StepBuilderFactory stepBuilderFactory;

    private final ObjectMapper objectMapper;

    @Autowired
    public RestMeterBatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, ObjectMapper objectMapper) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.objectMapper = objectMapper;
    }

    @Bean("restTemplate")
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean("customRestTemplate")
    RestTemplate customRestTemplate() {
        return new RestTemplate(new CustomHttpComponentsClientHttpRequestFactory());
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean("commandParametersMap")
    public ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Job getMeterJob(Step meterPointsByMeterParametersBatchStep, Step meterEventsStep, RabbitTemplate rabbitTemplate) {
        return jobBuilderFactory.get("pyramidJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobCompletionNotificationListener(rabbitTemplate))
                .start(meterPointsByMeterParametersBatchStep)
                .next(meterEventsStep)
                .build();
    }

    //endpoint GET /meterpointsbymeterparametersbatch/{parameterguid}/{dtfrom}/{dtto}
    // + extra body in GET request with {meterguid} list with comma separator in it
    @Bean
    public Step meterPointsByMeterParametersBatchStep(@Qualifier("customRestTemplate") RestTemplate customRestTemplate, RabbitTemplate rabbitTemplate, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper) {
        return stepBuilderFactory.get("stepMeterPointsByMeterParametersBatchStep")
                .<List<Reading>, List<Reading>> chunk(chunkSize)
                .reader(new MeterPointsByMeterParametersBatchReader(pyramidRestUrl, customRestTemplate, commandParametersMap, objectMapper))
                .writer(new MeterPointsByMeterParametersBatchWriter(rabbitTemplate, objectMapper))
                .build();
    }

    //endpoint GET /meterevents/{meterguid}/{dtfrom}/{dtto}
    @Bean
    public Step meterEventsStep(@Qualifier("restTemplate") RestTemplate restTemplate, RabbitTemplate rabbitTemplate,
        ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper) {
        return stepBuilderFactory.get("stepMeterEvents")
                .<List<EndDeviceEvent>, List<EndDeviceEvent>>chunk(chunkSize)
                .reader(new MeterEventsReader(pyramidRestUrl, restTemplate, commandParametersMap, objectMapper))
                .writer(new MeterEventsWriter(rabbitTemplate, objectMapper))
                .build();
    }

}
