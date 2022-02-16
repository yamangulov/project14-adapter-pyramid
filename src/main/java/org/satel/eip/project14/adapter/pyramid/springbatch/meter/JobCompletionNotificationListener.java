package org.satel.eip.project14.adapter.pyramid.springbatch.meter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.satel.eip.project14.adapter.pyramid.domain.command.response.CommandResponse;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class JobCompletionNotificationListener extends JobExecutionListenerSupport {

    private final RabbitTemplate rabbitTemplateSuccessCommand;
    private String commandUuid;

    public JobCompletionNotificationListener(RabbitTemplate rabbitTemplateSuccessCommand) {
        this.rabbitTemplateSuccessCommand = rabbitTemplateSuccessCommand;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
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
        log.info(result);

        rabbitTemplateSuccessCommand.convertAndSend(result, m -> {
            m.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
            return m;
        });
    }
}
