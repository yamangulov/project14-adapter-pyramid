package org.satel.eip.project14.adapter.pyramid.springbatch.meter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.satel.eip.project14.adapter.pyramid.domain.command.response.CommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    static final Logger LOGGER = LoggerFactory.getLogger("elastic-logger");

    private final RabbitTemplate rabbitTemplate;
    private String exchange;
    private String successCommandRoutingKey;
    private String commandUuid;
    private String successCommandQueue;
    private String defaultQueue;

    public JobCompletionNotificationListener(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        this.exchange = jobExecution.getJobParameters().getString("exchange");
        this.successCommandRoutingKey = jobExecution.getJobParameters().getString("successCommandRoutingKey");
        this.successCommandQueue = jobExecution.getJobParameters().getString("successCommandQueue");
        this.defaultQueue = jobExecution.getJobParameters().getString("defaultQueue");
        this.commandUuid = jobExecution.getJobParameters().getString("externalJobId");
    }

    @SneakyThrows
    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            sendGetJobIsDoneMessage(null);
        } else {
            sendGetJobIsDoneMessage(jobExecution.getAllFailureExceptions()
                    .stream()
                    .map(throwable -> throwable.getMessage() + "\r\n" + ExceptionUtils.getStackTrace(throwable))
                    .collect(Collectors.joining("\r\n")));
        }
    }

    private void sendGetJobIsDoneMessage(String errorMsg) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        String result = mapper.writeValueAsString(
                new CommandResponse(commandUuid, null, errorMsg));

        MDC.put("commandUuid", commandUuid);
        MDC.put("operation", "Write command result");
        LOGGER.info(result);

        rabbitTemplate.setDefaultReceiveQueue(successCommandQueue);
        rabbitTemplate.convertAndSend(this.exchange, this.successCommandRoutingKey, result, m -> {
            m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return m;
        });
        rabbitTemplate.setDefaultReceiveQueue(defaultQueue);
    }
}
