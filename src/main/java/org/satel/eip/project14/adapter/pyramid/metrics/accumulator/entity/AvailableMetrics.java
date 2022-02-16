package org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity;

import java.util.Arrays;

public enum AvailableMetrics {
    COMMAND_LISTENER_INCOME_COMMANDS_TOTAL("_command_listener_income_commands_total", "tag", "Кол-во входных команд для адаптера Пирамиды из RabbitMQ"),
    COMMAND_LISTENER_JSON_PROCESSING_ERROR("_command_listener_json_processing_error", "tag", "Кол-во ошибочно сконвертированных входных комманд для адаптера Пирамиды из RabbitMQ"),
    COMMAND_LISTENER_COMMAND_READING_ERROR("_command_listener_command_reading_error", "tag", "Кол-во неизвестных ошибок при чтении входных команд для адаптера Пирамиды из RabbitMQ"),
    COMMAND_LISTENER_EMPTY_COMMAND_UUID_ERROR("_command_listener_empty_command_uuid_error", "tag", "Кол-во входных команд для адаптера Пирамиды из RabbitMQ с пустым uuid"),
    COMMAND_LISTENER_UNDEFINED_COMMAND_TYPE_ERROR("_command_listener_undefined_command_type_error", "tag", "Кол-во входных команд для адаптера Пирамиды из RabbitMQ с не заданным типом команды"),
    COMMAND_LISTENER_NOT_SUFFICIENT_COMMAND_TYPE_ERROR("_command_listener_not_sufficient_command_type_error", "tag", "Кол-во входных команд для адаптера Пирамиды из RabbitMQ с типом команды, не пригодным для Пирамиды"),
    BATCH_JOB_EXECUTING_ERROR_TOTAL("_batch_job_executing_error_total", "tag", "Кол-во ошибок при исполнении пакетного задания для одной входной команды адаптера Пирамиды"),
    BATCH_JOB_METER_POINTS_TOTAL("_batch_job_meter_point_total", "tag", "Кол-во Reading, полученное из Rest API Пирамиды"),
    BATCH_JOB_METER_POINTS_BATCH_REQUESTS_TOTAL("_batch_job_meter_points_batch_requests_total", "tag", "Кол-во пакетных запросов в Rest API Пирамиды для получения Reading"),
    BATCH_JOB_METER_POINTS_BATCH_REQUESTS_ERROR("_batch_job_meter_points_batch_requests_error", "tag", "Кол-во ошибочно сконвертированных ответов на пакетные запросы в Rest API Пирамиды для получения Reading"),
    BATCH_JOB_METER_POINTS_RETURNED_TOTAL("_batch_job_meter_points_returned_total", "tag", "Кол-во Reading, успешно возвращенных в RabbitMQ из Rest API Пирамиды"),
    BATCH_JOB_METER_POINTS_MAPPING_ERROR("_batch_job_meter_points_mapping_error", "tag", "Кол-во ошибок при маппинге Reading в json строку перед отправкой в RabbitMQ"),
    BATCH_JOB_END_DEVICE_EVENTS_BATCH_REQUESTS_TOTAL("_batch_job_end_device_events_batch_requests_total", "tag", "Кол-во пакетных запросов в Rest API Пирамиды для получения  EndDeviceEvent"),
    BATCH_JOB_END_DEVICE_EVENTS_BATCH_REQUESTS_ERROR("_batch_job_end_device_events_batch_requests_error", "tag", "Кол-во ошибочно сконвертированных ответов на пакетные запросы в Rest API Пирамиды для получения  EndDeviceEvent"),
    BATCH_JOB_END_DEVICE_EVENTS_RETURNED_TOTAL("_batch_job_end_device_events_returned_total", "tag", "Кол-во EndDeviceEvent, успешно возвращенных в RabbitMQ из Rest API Пирамиды"),
    BATCH_JOB_END_DEVICE_EVENTS_MAPPING_ERROR("_batch_job_end_device_events_mapping_error", "tag", "Кол-во ошибок при маппинге EndDeviceEvent в json строку перед отправкой в RabbitMQ")
    ;

    private final String metric;
    private final String tag;
    private final String description;


    AvailableMetrics(String metric, String tag, String description) {
        this.metric = metric;
        this.tag = tag;
        this.description = description;
    }

    public String getMetric() {
        return metric;
    }

    public String getTag() {
        return tag;
    }

    public String getDescription() {
        return description;
    }

    public static AvailableMetrics fromString(String text) {
        return Arrays.stream(AvailableMetrics.values())
                .filter(type -> type.name().equalsIgnoreCase(text))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No constant with text " + text + " found"));
    }


}
