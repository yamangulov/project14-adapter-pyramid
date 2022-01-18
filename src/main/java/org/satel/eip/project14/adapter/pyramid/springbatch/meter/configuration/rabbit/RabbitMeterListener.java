package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.satel.eip.project14.adapter.pyramid.domain.command.CommandType;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequestContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.response.CommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RabbitMeterListener {
    static final Logger LOGGER = LoggerFactory.getLogger(RabbitMeterListener.class);

    @Value("${rabbitmq.MetersUuids.exchange}")
    private String metersUuidsExchange;
    @Value("${rabbitmq.MetersUuids.routingKey}")
    private String metersUuidsRoutingKey;
    @Value("${rabbitmq.commands.queue}")
    private String defaultQueue;

    @Value("${rabbitmq.MeterReadings.queue}")
    private String meterReadingsQueue;
    @Value("${rabbitmq.MeterReadings.routingKey}")
    private String meterReadingsRoutingKey;

    @Value("${rabbitmq.Consolidations.queue}")
    private String consolidationsQueue;
    @Value("${rabbitmq.Consolidations.routingKey}")
    private String consolidationsRoutingKey;

    @Value("${rabbitmq.Events.queue}")
    private String eventsQueue;
    @Value("${rabbitmq.Events.routingKey}")
    private String eventsRoutingKey;

    @Value("${rabbitmq.BadCommand.exchange}")
    private String badCommandExchange;
    @Value("${rabbitmq.BadCommand.routingKey}")
    private String badCommandRoutingKey;
    @Value("${rabbitmq.BadCommand.queue}")
    private String badCommandQueue;

    @Value("${rabbitmq.SuccessCommand.exchange}")
    private String successCommandExchange;
    @Value("${rabbitmq.SuccessCommand.routingKey}")
    private String successCommandRoutingKey;
    @Value("${rabbitmq.SuccessCommand.queue}")
    private String successCommandQueue;

    final JobLauncher jobLauncher;
    final Job getMeterJob;

    private final RabbitTemplate rabbitTemplate;
    private final ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap;

    private final ObjectMapper objectMapper;

    private final MeterRegistry meterRegistry;
    //counter for incoming rabbitmq packages
    private Counter counter;

    @PostConstruct
    public void init() {
        counter =
                Counter.builder("income_rabbitmq_package")
                        .description("Income package got from rabbitmq")
                        .register(meterRegistry);
    }

    @Autowired
    public RabbitMeterListener(JobLauncher jobLauncher, Job getMeterJob, RabbitTemplate rabbitTemplate,
                               ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.jobLauncher = jobLauncher;
        this.getMeterJob = getMeterJob;
        this.rabbitTemplate = rabbitTemplate;
        this.commandParametersMap = commandParametersMap;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "${rabbitmq.commands.queue}"),
            exchange = @Exchange(value = "${rabbitmq.MetersUuids.exchange}"),
            key = "${rabbitmq.MetersUuids.routingKey}"
    ))
    public void listenPyramidCommands(String in) throws JobInstanceAlreadyCompleteException,
            JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, JsonProcessingException {

        counter.increment();

        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(in);
        } catch (Exception e) {
            LOGGER.error("BAD ON READING COMMAND: {}", e.getMessage());
            sendBadCommandResponse(null, "BAD ON READING COMMAND FROM RABBIT");
            return;
        }

        String commandUuid = rootNode.get("uuid") == null ? null : rootNode.get("uuid").asText();
        if (commandUuid == null || commandUuid.isEmpty()) {
            LOGGER.error("EMPTY COMMAND UUID");
            sendBadCommandResponse(commandUuid, "EMPTY COMMAND UUID");
            return;
        }
        MDC.put("commandUuid", commandUuid);
        LOGGER.info(in);

        CommandType commandType = CommandType.getCommandTypeByString(rootNode.get("commandType").asText());

        if (commandType == null) {
            LOGGER.error("UNKNOWN COMMAND {} GOT FROM RABBITMQ", commandType);
            sendBadCommandResponse(rootNode.get("commandType").asText(), "UNKNOWN COMMAND");
            return;
        }

        if (commandType == CommandType.GET_PYRAMID_METERS_REQUEST) {
            try {
                processGetPyramidMetersJob(objectMapper, in);
            } catch (Exception e) {
                LOGGER.error("ERROR ON EXECUTING COMMAND IN BATCH JOB {}", commandType);
                sendBadCommandResponse(commandType.getName(), "ERROR ON EXECUTING COMMAND IN BATCH JOB");
            }
        } else {
            LOGGER.error("NOT SUFFICIENT FOR PYRAMID COMMAND {} GOT FROM RABBITMQ", commandType);
            sendBadCommandResponse(rootNode.get("commandType").asText(), "NOT SUFFICIENT FOR PYRAMID COMMAND");
        }

    }

    private void processGetPyramidMetersJob(ObjectMapper mapper, String in) throws JsonProcessingException,
            JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
            JobParametersInvalidException, JobRestartException {
        GetMeterRequestCommand command = mapper.readValue(in, GetMeterRequestCommand.class);

        String externalJobId = command.getUuid().toString();
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("dateNow", LocalDateTime.now().toString())
                .addString("externalJobId", externalJobId)
                .addString("exchange", this.metersUuidsExchange)
                .addString("routingKey", this.metersUuidsRoutingKey)
                .addString("readingsRoutingKey", this.meterReadingsRoutingKey)
                .addString("meterReadingsQueue", this.meterReadingsQueue)
                .addString("consolidationsRoutingKey", this.consolidationsRoutingKey)
                .addString("consolidationsQueue", this.consolidationsQueue)
                .addString("eventsRoutingKey", this.eventsRoutingKey)
                .addString("eventsQueue", this.eventsQueue)
                .addString("defaultQueue", defaultQueue)
                .addString("successCommandRoutingKey", this.successCommandRoutingKey)
                .addString("successCommandQueue", this.successCommandQueue)
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
        LOGGER.info(result);

        rabbitTemplate.setDefaultReceiveQueue(badCommandQueue);
        rabbitTemplate.convertAndSend(this.badCommandExchange, this.badCommandRoutingKey, result, m -> {
            m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return m;
        });
        rabbitTemplate.setDefaultReceiveQueue(defaultQueue);
    }

}