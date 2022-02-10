package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@EnableRabbit
@Configuration
@RefreshScope
public class RabbitConfig {
    @Value("${rabbitmq.MetersUuids.exchange}")
    private String defaultExchange;

    @Value("${rabbitmq.MetersUuids.routingKey}")
    private String metersUuidsRoutingKey;
    @Value("${rabbitmq.commands.queue}")
    private String metersUuidsQueue;

    @Value("${rabbitmq.BadCommand.exchange}")
    private String badCommandExchange;
    @Value("${rabbitmq.BadCommand.routingKey}")
    private String badCommandRoutingKey;
    @Value("${rabbitmq.BadCommand.queue}")
    private String badCommandQueue;

    @Value("${rabbitmq.Consolidations.queue}")
    private String consolidationsQueue;
    @Value("${rabbitmq.Consolidations.routingKey}")
    private String consolidationsRoutingKey;

    @Value("${rabbitmq.Events.queue}")
    private String eventsQueue;
    @Value("${rabbitmq.Events.routingKey}")
    private String eventsRoutingKey;

    @Value("${rabbitmq.SuccessCommand.routingKey}")
    private String successCommandRoutingKey;
    @Value("${rabbitmq.SuccessCommand.queue}")
    private String successCommandQueue;

    @Value("${rabbitmq.MeterReadings.queue}")
    private String meterReadingsQueue;
    @Value("${rabbitmq.MeterReadings.routingKey}")
    private String meterReadingsRoutingKey;

    @Value("${spring.rabbitmq.host}")
    String host;

    @Value("${spring.rabbitmq.port}")
    String port;

    @Value("${spring.rabbitmq.username}")
    String username;

    @Value("${spring.rabbitmq.password}")
    String password;

    @Value("${spring.rabbitmq.virtual-host}")
    String virtualHost;

    @Bean
    @RefreshScope
    public SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory() {
        SimpleRabbitListenerContainerFactory simpleRabbitListenerContainerFactory = new SimpleRabbitListenerContainerFactory();
        simpleRabbitListenerContainerFactory.setConnectionFactory(listenerConnectionFactory());
        return simpleRabbitListenerContainerFactory;
    }

    @Bean("connectionFactory")
    @RefreshScope
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, Integer.parseInt(port));
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        return connectionFactory;
    }

    @Bean("listenerConnectionFactory")
    @Primary
    @RefreshScope
    public ConnectionFactory listenerConnectionFactory() {
        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, Integer.parseInt(port));
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setVirtualHost(virtualHost);
        return connectionFactory;
    }

    @Bean("rabbitTemplate")
    @Primary
    @RefreshScope
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(listenerConnectionFactory());
    }

    @Bean("rabbitTemplateBadCommand")
    @RefreshScope
    public RabbitTemplate rabbitTemplateBadCommand() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(badCommandQueue);
        rabbitTemplate.setExchange(badCommandExchange);
        rabbitTemplate.setRoutingKey(badCommandRoutingKey);
        return rabbitTemplate;
    }

    @Bean("rabbitTemplateSuccessCommand")
    @RefreshScope
    public RabbitTemplate rabbitTemplateSuccessCommand() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(successCommandQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(successCommandRoutingKey);
        return rabbitTemplate;
    }

    @Bean("rabbitTemplateConsolidations")
    @RefreshScope
    public RabbitTemplate rabbitTemplateConsolidations() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(consolidationsQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(consolidationsRoutingKey);
        return rabbitTemplate;
    }

    @Bean("rabbitTemplateEvents")
    @RefreshScope
    public RabbitTemplate rabbitTemplateEvents() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(eventsQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(eventsRoutingKey);
        return rabbitTemplate;
    }

    @Bean("rabbitTemplateMeterReadings")
    @RefreshScope
    public RabbitTemplate rabbitTemplateMeterReadings() {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory());
        rabbitTemplate.setDefaultReceiveQueue(meterReadingsQueue);
        rabbitTemplate.setExchange(defaultExchange);
        rabbitTemplate.setRoutingKey(meterReadingsRoutingKey);
        return rabbitTemplate;
    }

    @Bean("listenerAmqpAdmin")
    @RefreshScope
    public AmqpAdmin listenerAmqpAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(listenerConnectionFactory());
        if (!inputCommandQueue().getName().equals("PYRAMID_METERS.COMMANDS")) {
            log.error("Очередь {} имеет некорректное имя, проверьте сервер RabbitMQ, при полном перезапуске " +
                    "адаптера Пирамиды будет создана паразитная очередь с таким именем, и ее нужно удалить " +
                    "и создать очередь с правильным именем", inputCommandQueue());
        }
        return rabbitAdmin;
    }

    @Bean("amqpAdmin")
    @RefreshScope
    public AmqpAdmin amqpAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    @RefreshScope
    public DirectExchange commandExchange() {
        return new DirectExchange(defaultExchange);
    }

    @Bean("inputCommandQueue")
    @RefreshScope
    public Queue inputCommandQueue() {
        return new Queue(metersUuidsQueue);
    }

    @Bean
    @RefreshScope
    public Binding commandBinding() {
        return BindingBuilder.bind(inputCommandQueue()).to(commandExchange()).with(metersUuidsRoutingKey);
    }

}
