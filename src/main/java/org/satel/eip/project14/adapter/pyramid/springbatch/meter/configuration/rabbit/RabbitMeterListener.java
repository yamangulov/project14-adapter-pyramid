package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.domain.command.CommandType;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequestContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.response.CommandResponse;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.AccumulatorService;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity.AvailableMetrics;
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

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

import static org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit.RabbitConstant.METERS_UUIDS_QUEUE;

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
    private final AccumulatorService accumulatorService;

    @Autowired
    public RabbitMeterListener(JobLauncher jobLauncher, Job getMeterJob, @Qualifier("rabbitTemplateBadCommand") RabbitTemplate rabbitTemplateBadCommand, ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ObjectMapper objectMapper, AccumulatorService accumulatorService) {
        this.jobLauncher = jobLauncher;
        this.getMeterJob = getMeterJob;
        this.rabbitTemplateBadCommand = rabbitTemplateBadCommand;
        this.commandParametersMap = commandParametersMap;
        this.objectMapper = objectMapper;
        this.accumulatorService = accumulatorService;
    }

    public void pyramidCommandProcessor(String in) throws JsonProcessingException {
        JsonNode rootNode;
        try {
            rootNode = objectMapper.readTree(in);
        } catch (Exception e) {
            log.error("BAD ON READING COMMAND: {}", e.getMessage());
            sendBadCommandResponse(null, "BAD ON READING COMMAND FROM RABBIT");
            accumulatorService.increment(accumulatorService.getChannel("commandListener"), AvailableMetrics.COMMAND_LISTENER_COMMAND_READING_ERROR);
            return;
        }

        String commandUuid = rootNode.get("uuid") == null ? null : rootNode.get("uuid").asText();
        if (commandUuid == null || commandUuid.isEmpty()) {
            log.error("EMPTY COMMAND UUID");
            sendBadCommandResponse(commandUuid, "EMPTY COMMAND UUID");
            accumulatorService.increment(accumulatorService.getChannel("commandListener"), AvailableMetrics.COMMAND_LISTENER_EMPTY_COMMAND_UUID_ERROR);
            return;
        }
        MDC.put("commandUuid", commandUuid);
        log.info(in);

        CommandType commandType = CommandType.getCommandTypeByString(rootNode.get("commandType").asText());

        if (commandType == null) {
            log.error("UNDEFINED COMMAND {} GOT FROM RABBITMQ", commandType);
            sendBadCommandResponse(rootNode.get("commandType").asText(), "UNDEFINED COMMAND");
            accumulatorService.increment(accumulatorService.getChannel("commandListener"), AvailableMetrics.COMMAND_LISTENER_UNDEFINED_COMMAND_TYPE_ERROR);
            return;
        }

        if (commandType == CommandType.GET_PYRAMID_METERS_REQUEST) {
            try {
                processGetPyramidMetersJob(objectMapper, in);
            } catch (Exception e) {
                log.error("ERROR ON EXECUTING COMMAND IN BATCH JOB {}", commandType);
                sendBadCommandResponse(commandType.getName(), "ERROR ON EXECUTING COMMAND IN BATCH JOB");
                accumulatorService.increment(accumulatorService.getChannel("batchJob"), AvailableMetrics.BATCH_JOB_EXECUTING_ERROR_TOTAL);
                e.printStackTrace();
            }
        } else {
            log.error("NOT SUFFICIENT FOR PYRAMID COMMAND {} GOT FROM RABBITMQ", commandType);
            sendBadCommandResponse(rootNode.get("commandType").asText(), "NOT SUFFICIENT FOR PYRAMID COMMAND");
            accumulatorService.increment(accumulatorService.getChannel("commandListener"), AvailableMetrics.COMMAND_LISTENER_NOT_SUFFICIENT_COMMAND_TYPE_ERROR);
        }

    }

    public void pyramidCommandListener(Message message) {
        String in = new String(message.getBody());
        accumulatorService.increment(accumulatorService.getChannel("commandListener"), AvailableMetrics.COMMAND_LISTENER_INCOME_COMMANDS_TOTAL);
        try {
            pyramidCommandProcessor(in);
        } catch (JsonProcessingException e) {
            log.error("Ошибка при конвертации в строку входящей команды из очереди {}", metersUuidsQueue);
            accumulatorService.increment(accumulatorService.getChannel("commandListener"), AvailableMetrics.COMMAND_LISTENER_JSON_PROCESSING_ERROR);
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

}