package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration;

import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.springbatch.JobCompletionNotificationListener;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.reader.RestMeterReader;
import org.satel.eip.project14.adapter.pyramid.springbatch.meter.writer.FullMeterWriter;
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
    public ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Job getMeterJob(Step meterStep) {
        return jobBuilderFactory.get("importMeterJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobCompletionNotificationListener())
                .start(meterStep)
                .build();
    }

    @Bean
    public Step meterStep(RestTemplate restTemplate, RabbitTemplate rabbitTemplate) {
        return stepBuilderFactory.get("stepMeter")
                .<String, String>chunk(chunkSize)
                .reader(new RestMeterReader(pyramidRestUrl, restTemplate))
                .writer(new FullMeterWriter(rabbitTemplate))
                .build();
    }
}
