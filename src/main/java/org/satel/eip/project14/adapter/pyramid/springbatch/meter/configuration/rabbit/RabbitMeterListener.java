package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.satel.eip.project14.adapter.pyramid.domain.command.CommandType;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.CommandParametersContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.container.GetMeterRequestContainer;
import org.satel.eip.project14.adapter.pyramid.domain.command.entity.GetMeterRequestCommand;
import org.satel.eip.project14.adapter.pyramid.domain.command.response.GetMeterRequestCommandResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class RabbitMeterListener {
    static final Logger LOGGER = LoggerFactory.getLogger(RabbitMeterListener.class);

    @Value("${rabbitmq.MetersUuids.exchange}")
    private String metersUuidsExchange;
    @Value("${rabbitmq.MetersUuids.routingKey}")
    private String metersUuidsRoutingKey;
    @Value("${rabbitmq.Meters.exchange}")
    private String metersExchange;

    @Value("${rabbitmq.Meters.routingKey}")
    private String metersRoutingKey;

    final JobLauncher jobLauncher;
    final Job getMeterJob;
    private final RabbitTemplate rabbitTemplate;
    private final ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap;
    private final ConcurrentHashMap<String, Map<String, String>> stepsResultsMap;

    @Autowired
    public RabbitMeterListener(JobLauncher jobLauncher, Job getMeterJob, RabbitTemplate rabbitTemplate,
                               ConcurrentHashMap<String, CommandParametersContainer<?>> commandParametersMap, ConcurrentHashMap<String, Map<String, String>> stepsResultsMap) {
        this.jobLauncher = jobLauncher;
        this.getMeterJob = getMeterJob;
        this.rabbitTemplate = rabbitTemplate;
        this.commandParametersMap = commandParametersMap;
        this.stepsResultsMap = stepsResultsMap;
    }

    @RabbitListener(queues = "${rabbitmq.commands.queue}")
    public void listenSoapPartnersCommands(String in) throws JobInstanceAlreadyCompleteException,
            JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(in);

        CommandType commandType = CommandType.getCommandTypeByString(rootNode.get("commandType").asText());

        if (commandType == CommandType.GET_PYRAMID_METERS_REQUEST) {
            processGetPyramidMetersJob(mapper, in);
        } else {
            LOGGER.error("UNKNOWN COMMAND {} GOT FROM RABBITMQ", commandType);
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
                .toJobParameters();

        commandParametersMap.put(externalJobId, new GetMeterRequestContainer(command));
        stepsResultsMap.put(externalJobId, new ConcurrentHashMap<>());
        jobLauncher.run(getMeterJob, jobParameters);

        sendGetMeterJobIsDoneMessage(externalJobId);
    }

    private void sendGetMeterJobIsDoneMessage(String externalJobId) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        rabbitTemplate.convertAndSend(this.metersExchange, this.metersRoutingKey,
                mapper.writeValueAsString(new GetMeterRequestCommandResponse(null, externalJobId)));
    }

}