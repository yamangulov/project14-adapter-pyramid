package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import static org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit.RabbitConstant.*;

@Slf4j
@EnableRabbit
@Configuration
@RefreshScope
public class RabbitConfig {
    @Value(DEFAULT_EXCHANGE)
    private String defaultExchange;

    @Value(METERS_UUIDS_ROUTING_KEY)
    private String metersUuidsRoutingKey;
    @Value(METERS_UUIDS_QUEUE)
    private String metersUuidsQueue;

    @Value(BAD_COMMAND_EXCHANGE)
    private String badCommandExchange;
    @Value(BAD_COMMAND_ROUTING_KEY)
    private String badCommandRoutingKey;
    @Value(BAD_COMMAND_QUEUE)
    private String badCommandQueue;

    @Value(CONSOLIDATIONS_QUEUE)
    private String consolidationsQueue;
    @Value(CONSOLIDATIONS_ROUTING_KEY)
    private String consolidationsRoutingKey;

    @Value(EVENTS_QUEUE)
    private String eventsQueue;
    @Value(EVENTS_ROUTING_KEY)
    private String eventsRoutingKey;

    @Value(SUCCESS_COMMAND_ROUTING_KEY)
    private String successCommandRoutingKey;
    @Value(SUCCESS_COMMAND_QUEUE)
    private String successCommandQueue;

    @Value(METER_READINGS_QUEUE)
    private String meterReadingsQueue;
    @Value(METER_READINGS_ROUTING_KEY)
    private String meterReadingsRoutingKey;

    @Value(RABBITMQ_HOST)
    String host;

    @Value(RABBITMQ_PORT)
    String port;

    @Value(RABBITMQ_USER_NAME)
    String username;

    @Value(RABBITMQ_PASSWORD)
    String password;

    @Value(RABBITMQ_VIRTUAL_HOST)
    String virtualHost;

    @Bean
    @RefreshScope
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        simpleRabbitListenerContainerFactory.setConnectionFactory(connectionFactory());
        return simpleRabbitListenerContainerFactory;
    }

    @Bean
    @RefreshScope
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, Integer.parseInt(port));
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        return connectionFactory;
    }

    @Bean(LISTENER_RABBIT_TEMPLATE)
    @Primary
    @RefreshScope
    public RabbitTemplate rabbitTemplate() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(metersUuidsQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(metersUuidsRoutingKey);
        return rabbitTemplate;
    }

    @Bean(BAD_COMMAND_RABBIT_TEMPLATE)
    @RefreshScope
    public RabbitTemplate rabbitTemplateBadCommand() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(badCommandQueue);
        rabbitTemplate.setExchange(badCommandExchange);
        rabbitTemplate.setRoutingKey(badCommandRoutingKey);
        return rabbitTemplate;
    }

    @Bean(SUCCESS_COMMAND_RABBIT_TEMPLATE)
    @RefreshScope
    public RabbitTemplate rabbitTemplateSuccessCommand() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(successCommandQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(successCommandRoutingKey);
        return rabbitTemplate;
    }

    @Bean(CONSOLIDATIONS_RABBIT_TEMPLATE)
    @RefreshScope
    public RabbitTemplate rabbitTemplateConsolidations() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(consolidationsQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(consolidationsRoutingKey);
        return rabbitTemplate;
    }

    @Bean(EVENTS_RABBIT_TEMPLATE)
    @RefreshScope
    public RabbitTemplate rabbitTemplateEvents() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(eventsQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(eventsRoutingKey);
        return rabbitTemplate;
    }

    @Bean(METER_READINGS_RABBIT_TEMPLATE)
    @RefreshScope
    public RabbitTemplate rabbitTemplateMeterReadings() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(meterReadingsQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(meterReadingsRoutingKey);
        return rabbitTemplate;
    }

}
