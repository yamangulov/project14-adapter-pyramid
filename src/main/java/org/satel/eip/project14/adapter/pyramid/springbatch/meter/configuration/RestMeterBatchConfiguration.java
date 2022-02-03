package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAccumulator;

@Configuration
@EnableBatchProcessing
@EnableScheduling
@RefreshScope
public class RestMeterBatchConfiguration {
    @Value("${pyramid.rest.url}")
    private String pyramidRestUrl;

    @Value("${commands.GetMeter.limit:20}")
    private int chunkSize;

    final JobBuilderFactory jobBuilderFactory;
    final StepBuilderFactory stepBuilderFactory;

    private final ObjectMapper objectMapper;

    private final MeterRegistry meterRegistry;
    private Counter outCounter;
    private Gauge outGauge;

    @PostConstruct
    public void init() {
        outCounter =
                Counter.builder("outcome_rabbitmq_package")
                        .description("Outcome package got from rabbitmq")
                        .register(meterRegistry);
        outGauge =
                Gauge.builder("outcome_rabbitmq_package_resetable", this::getOutGaugeCounter)
                .description("Gauge for outcome message got from RabbitMQ")
                .register(meterRegistry);
    }

    @Autowired
    public RestMeterBatchConfiguration(JobBuilderFactory jobBuilderFactory, StepBuilderFactory stepBuilderFactory, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.jobBuilderFactory = jobBuilderFactory;
        this.stepBuilderFactory = stepBuilderFactory;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Bean("outGauge")
    public Gauge outGauge() {
        return outGauge;
    }

    // Реализацию этого класса или другого, используемого на его месте, нужно делать потокобезопасным
    @Bean("outGaugeCounter")
    public DoubleAccumulator outGaugeCounter() {
        return new DoubleAccumulator(Double::sum, 0);
    }

    private Double getOutGaugeCounter() {
        return outGaugeCounter().get();
    }

    @Bean("outCounter")
    Counter outCounter() {
        return this.outCounter;
    }

    @Bean("restTemplate")
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // @RefreshScope не работает в этом классе, если оставить дефолтовое имя RestTemplate, так как с @RefreshScope
    // Spring создает два шаблона с одинаковым именем - из автоконфигурации Spring и из данного кастомного определения бина
    @Bean("customRestTemplate")
    RestTemplate customRestTemplate() {
        return new RestTemplate(new CustomHttpComponentsClientHttpRequestFactory());
    }

    @Bean("customRabbitTemplate")
    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }

    @Bean("commandParametersMap")
    public ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    @RefreshScope
    public Job getMeterJob(Step meterPointsByMeterParametersBatchStep, Step meterEventsStep, RabbitTemplate customRabbitTemplate) {
        return jobBuilderFactory.get("pyramidJob")
                .incrementer(new RunIdIncrementer())
                .listener(new JobCompletionNotificationListener(customRabbitTemplate))
                .start(meterPointsByMeterParametersBatchStep)
                .next(meterEventsStep)
                .build();
    }

    //endpoint GET /meterpointsbymeterparametersbatch/{parameterguid}/{dtfrom}/{dtto}
    // + extra body in GET request with {meterguid} list with comma separator in it
    @Bean
    @RefreshScope
    public Step meterPointsByMeterParametersBatchStep(@Qualifier("customRestTemplate") RestTemplate customRestTemplate, RabbitTemplate customRabbitTemplate, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper, @Qualifier("outCounter") Counter outCounter, @Qualifier("outGaugeCounter") DoubleAccumulator outGaugeCounter, @Qualifier("outGauge") Gauge outGauge) {
        return stepBuilderFactory.get("stepMeterPointsByMeterParametersBatchStep")
                .<List<Reading>, List<Reading>> chunk(chunkSize)
                .reader(new MeterPointsByMeterParametersBatchReader(pyramidRestUrl, customRestTemplate, commandParametersMap, objectMapper))
                .writer(new MeterPointsByMeterParametersBatchWriter(customRabbitTemplate, objectMapper, outCounter, outGaugeCounter, outGauge))
                .build();
    }

    //endpoint GET /meterevents/{meterguid}/{dtfrom}/{dtto}
    @Bean
    @RefreshScope
    public Step meterEventsStep(@Qualifier("restTemplate") RestTemplate restTemplate, RabbitTemplate customRabbitTemplate,
        ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper, @Qualifier("outCounter") Counter outCounter, @Qualifier("outGaugeCounter") DoubleAccumulator outGaugeCounter, @Qualifier("outGauge") Gauge outGauge) {
        return stepBuilderFactory.get("stepMeterEvents")
                .<List<EndDeviceEvent>, List<EndDeviceEvent>>chunk(chunkSize)
                .reader(new MeterEventsReader(pyramidRestUrl, restTemplate, commandParametersMap, objectMapper))
                .writer(new MeterEventsWriter(customRabbitTemplate, objectMapper, outCounter, outGaugeCounter, outGauge))
                .build();
    }

    @Scheduled(fixedDelayString = "60000")
    private void clearGaugeCounter() {
        outGaugeCounter().reset();
        outGauge.measure();
    }

}
