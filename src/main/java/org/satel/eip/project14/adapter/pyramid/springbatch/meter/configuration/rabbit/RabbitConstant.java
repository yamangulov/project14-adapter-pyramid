package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import org.springframework.stereotype.Service;

@Service
public final class RabbitConstant {
    public static final String DEFAULT_EXCHANGE = "${rabbitmq.MetersUuids.exchange}";
    public static final String METERS_UUIDS_ROUTING_KEY = "${rabbitmq.MetersUuids.routingKey}";
    public static final String METERS_UUIDS_QUEUE = "${rabbitmq.commands.queue}";
    public static final String BAD_COMMAND_EXCHANGE = "${rabbitmq.BadCommand.exchange}";
    public static final String BAD_COMMAND_ROUTING_KEY = "${rabbitmq.BadCommand.routingKey}";
    public static final String BAD_COMMAND_QUEUE = "${rabbitmq.BadCommand.queue}";
    public static final String CONSOLIDATIONS_QUEUE = "${rabbitmq.Consolidations.queue}";
    public static final String CONSOLIDATIONS_ROUTING_KEY = "${rabbitmq.Consolidations.routingKey}";
    public static final String EVENTS_QUEUE = "${rabbitmq.Events.queue}";
    public static final String EVENTS_ROUTING_KEY = "${rabbitmq.Events.routingKey}";
    public static final String SUCCESS_COMMAND_ROUTING_KEY = "${rabbitmq.SuccessCommand.routingKey}";
    public static final String SUCCESS_COMMAND_QUEUE = "${rabbitmq.SuccessCommand.queue}";
    public static final String METER_READINGS_QUEUE = "${rabbitmq.MeterReadings.queue}";
    public static final String METER_READINGS_ROUTING_KEY = "${rabbitmq.MeterReadings.routingKey}";
    public static final String RABBITMQ_HOST = "${spring.rabbitmq.host}";
    public static final String RABBITMQ_PORT = "${spring.rabbitmq.port}";
    public static final String RABBITMQ_USER_NAME = "${spring.rabbitmq.username}";
    public static final String RABBITMQ_PASSWORD = "${spring.rabbitmq.password}";
    public static final String RABBITMQ_VIRTUAL_HOST = "${spring.rabbitmq.virtual-host}";
    public static final String LISTENER_RABBIT_TEMPLATE = "rabbitTemplate";
    public static final String BAD_COMMAND_RABBIT_TEMPLATE = "rabbitTemplateBadCommand";
    public static final String SUCCESS_COMMAND_RABBIT_TEMPLATE = "rabbitTemplateSuccessCommand";
    public static final String CONSOLIDATIONS_RABBIT_TEMPLATE = "rabbitTemplateConsolidations";
    public static final String EVENTS_RABBIT_TEMPLATE = "rabbitTemplateEvents";
    public static final String METER_READINGS_RABBIT_TEMPLATE = "rabbitTemplateMeterReadings";
    public static final String PYRAMID_REST_URI = "${pyramid.rest.url}";
    public static final String CHUNK_SIZE = "${commands.GetMeter.limit:20}";

}
