package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration;

import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.springbatch.JobCompletionNotificationListener;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterEventsDetailReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterEventsReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterParametresWithStatusReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterPointReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.MeterReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.ReadingReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterEventsDetailWriter;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterEventsWriter;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterParametresWithStatusWriter;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterPointWriter;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.MeterWriter;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.ReadingWriter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
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

    @Autowired
    public RestMeterBatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
    }

    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean("commandParametersMap")
    public ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap() {
        return new ConcurrentHashMap<>();
    }

    // для хранения результатов промежуточных запросов к Rest API Пирамиды и передачи их между Spring Batch steps
    // writer() в таких steps пишут результаты именно сюда, и только когда все данные получены - в RabbitMQ
    @Bean("stepsResultsMap")
    public ConcurrentHashMap<String, Map<Integer, Object>> stepsResultsMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Job getMeterJob(Step meterParametersWithStatusStep, Step meterEventsStep, Step meterEventsDetailStep,
                           Step meterStep, Step meterPointStep, Step readingStep) {
        return jobBuilderFactory.get("pyramidJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobCompletionNotificationListener())
                .start(meterParametersWithStatusStep)
                .next(meterEventsStep)
                .next(meterEventsDetailStep)
                .next(meterStep)
                .next(meterPointStep)
                .next(readingStep)
                .build();
    }

    //endpoint GET /meterparameterswithstatus/{meterguid}/{parameterguid}/{dtfrom}/{dtto}
    @Bean
    public Step meterParametersWithStatusStep(RestTemplate restTemplate, RabbitTemplate rabbitTemplate, ConcurrentHashMap<String, CommandParametersContainer<GetMeterRequestCommand>> commandParametersMap, ConcurrentHashMap<String, Map<Integer, Object>> stepsResultsMap) {
        return stepBuilderFactory.get("stepMeterParametersWithStatus")
                .<Map<String, List<String>>, Map<String, List<String>>>chunk(chunkSize)
                .reader(new MeterParametresWithStatusReader(pyramidRestUrl, restTemplate, commandParametersMap))
                .writer(new MeterParametresWithStatusWriter(rabbitTemplate, stepsResultsMap, commandParametersMap))
                .build();
    }

    //endpoint GET /meterevents/{meterguid}/{dtfrom}/{dtto}
    @Bean
    public Step meterEventsStep(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        return stepBuilderFactory.get("stepMeterEvents")
                .<String, String>chunk(chunkSize)
                .reader(new MeterEventsReader(pyramidRestUrl, restTemplate))
                .writer(new MeterEventsWriter(rabbitTemplate))
                .build();
    }

    //endpoint GET /object/{objectGuid} для событий приборов учета
    @Bean
    public Step meterEventsDetailStep(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        return stepBuilderFactory.get("stepMeterEventsDetail")
                .<String, String>chunk(chunkSize)
                .reader(new MeterEventsDetailReader(pyramidRestUrl, restTemplate))
                .writer(new MeterEventsDetailWriter(rabbitTemplate))
                .build();
    }

    //endpoint GET /meter/{meterguid}/
    @Bean
    public Step meterStep(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        return stepBuilderFactory.get("stepMeter")
                .<String, String>chunk(chunkSize)
                .reader(new MeterReader(pyramidRestUrl, restTemplate))
                .writer(new MeterWriter(rabbitTemplate))
                .build();
    }

    //endpoint GET /object/{objectGuid} для получения тарифа ТУ
    @Bean
    public Step meterPointStep(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        return stepBuilderFactory.get("stepMeterPoint")
                .<String, String>chunk(chunkSize)
                .reader(new MeterPointReader(pyramidRestUrl, restTemplate))
                .writer(new MeterPointWriter(rabbitTemplate))
                .build();
    }

    //endpoint GET /meterpointparameterswithstatus/{meterpointguid}/{parameterguid}/{dtfrom}/{dtto}
    @Bean
    public Step readingStep(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        return stepBuilderFactory.get("stepReading")
                .<String, String>chunk(chunkSize)
                .reader(new ReadingReader(pyramidRestUrl, restTemplate))
                .writer(new ReadingWriter(rabbitTemplate))
                .build();
    }
}
