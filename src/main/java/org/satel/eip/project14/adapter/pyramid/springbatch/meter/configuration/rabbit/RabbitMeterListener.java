package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.domain.command.CommandType;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequestContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.response.CommandResponse;
import org.slf4j.MDC;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAccumulator;

import static org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit.RabbitConstant.*;

@Slf4j
@Configuration
@EnableScheduling
@RefreshScope
public class RabbitMeterListener {


    @Value(METERS_UUIDS_QUEUE)
    private String metersUuidsQueue;

    final JobLauncher jobLauncher;
    final Job getMeterJob;

    private final RabbitTemplate rabbitTemplateBadCommand;
    private final ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap;

    private final ObjectMapper objectMapper;

    private final MeterRegistry meterRegistry;
    private Counter inCounter;
    private Gauge inGauge;

    @PostConstruct
    public void init() {
        inCounter =
                Counter.builder("income_rabbitmq_package")
                        .description("Income package got from RabbitMQ")
                        .register(meterRegistry);
        inGauge = Gauge.builder("income_rabbitmq_package_resetable", this::getInGaugeCounter)
                .description("Gauge for income message got from RabbitMQ")
                .register(meterRegistry);
    }

    @Autowired
    public RabbitMeterListener(JobLauncher jobLauncher, Job getMeterJob, @Qualifier("rabbitTemplateBadCommand") RabbitTemplate rabbitTemplateBadCommand, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.jobLauncher = jobLauncher;
        this.getMeterJob = getMeterJob;
        this.rabbitTemplateBadCommand = rabbitTemplateBadCommand;
        this.commandParametersMap = commandParametersMap;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @Bean("inGaugeCounter")
    DoubleAccumulator inGaugeCounter() {
        return new DoubleAccumulator(Double::sum, 0);
    }

    Double getInGaugeCounter() {
        return inGaugeCounter().get();
    }
    
    @Bean("inCounter")
    Counter inCounter() {
        return this.inCounter;
    }

    // ???????? ???????????????????? ???????????????????? ???????? ?? ???????? SimpleMessageListenerContainer
    //    @RabbitListener(id = "pyramidCommandListener", bindings = @QueueBinding(
    //            value = @Queue(value = "${rabbitmq.commands.queue}"),
    //            exchange = @Exchange(value = "${rabbitmq.MetersUuids.exchange}"),
    //            key = "${rabbitmq.MetersUuids.routingKey}"
    //    ))
    public void listenPyramidCommands(String in) throws JsonProcessingException {

        inCounter.increment();
        inGaugeCounter().accumulate(1.0);
        inGauge.measure();

        log.info("inCounter: {}", inCounter.count());

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(in);
        } catch (Exception e) {
            log.error("BAD ON READING COMMAND: {}", e.getMessage());
            sendBadCommandResponse(null, "BAD ON READING COMMAND FROM RABBIT");
            return;
        }

        String commandUuid = rootNode.get("uuid") == null ? null : rootNode.get("uuid").asText();
        if (commandUuid == null || commandUuid.isEmpty()) {
            log.error("EMPTY COMMAND UUID");
            sendBadCommandResponse(commandUuid, "EMPTY COMMAND UUID");
            return;
        }
        MDC.put("commandUuid", commandUuid);
        log.info(in);

        CommandType commandType = CommandType.getCommandTypeByString(rootNode.get("commandType").asText());

        if (commandType == null) {
            log.error("UNKNOWN COMMAND {} GOT FROM RABBITMQ", commandType);
            sendBadCommandResponse(rootNode.get("commandType").asText(), "UNKNOWN COMMAND");
            return;
        }

        if (commandType == CommandType.GET_PYRAMID_METERS_REQUEST) {
            try {
                processGetPyramidMetersJob(objectMapper, in);
            } catch (Exception e) {
                log.error("ERROR ON EXECUTING COMMAND IN BATCH JOB {}", commandType);
                sendBadCommandResponse(commandType.getName(), "ERROR ON EXECUTING COMMAND IN BATCH JOB");
                e.printStackTrace();
            }
        } else {
            log.error("NOT SUFFICIENT FOR PYRAMID COMMAND {} GOT FROM RABBITMQ", commandType);
            sendBadCommandResponse(rootNode.get("commandType").asText(), "NOT SUFFICIENT FOR PYRAMID COMMAND");
        }

    }

    public void pyramidCommandListener(Message message) {
        String in = new String(message.getBody());
        try {
            listenPyramidCommands(in);
        } catch (JsonProcessingException e) {
            log.error("???????????? ?????? ?????????????????????? ?? ???????????? ???????????????? ?????????????? ???? ?????????????? {}", metersUuidsQueue);
        }
    }

    @Bean
    @RefreshScope
    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory) {
        SimpleMessageListenerContainer simpleMessageListenerContainer = new SimpleMessageListenerContainer(connectionFactory);
        simpleMessageListenerContainer.addQueues(new Queue(metersUuidsQueue));
        simpleMessageListenerContainer.setMessageListener(this::pyramidCommandListener);
        simpleMessageListenerContainer.start();
        return simpleMessageListenerContainer;
    }

    private void processGetPyramidMetersJob(ObjectMapper mapper, String in) throws JsonProcessingException,
            JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
            JobParametersInvalidException, JobRestartException {
        GetMeterRequestCommand command = mapper.readValue(in, GetMeterRequestCommand.class);

        String externalJobId = command.getUuid().toString();
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dateNow", LocalDateTime.now().toString())
                .addString("externalJobId", externalJobId)
                .toJobParameters();

        commandParametersMap.put(externalJobId, new GetMeterRequestContainer(command));
        jobLauncher.run(getMeterJob, jobParameters);

    }

    private void sendBadCommandResponse(String externalJobId, String errorMsg) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String result = mapper.writeValueAsString(
                new CommandResponse(externalJobId, null, errorMsg));

        MDC.put("commandUuid", externalJobId);
        MDC.put("operation", "Write command result");
        log.info(result);

        rabbitTemplateBadCommand.convertAndSend(result, m -> {
            m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return m;
        });
    }

    @Scheduled(fixedDelayString = "60000")
    private void clearGaugeCounter() {
        inGaugeCounter().reset();
        inGauge.measure();
    }

}