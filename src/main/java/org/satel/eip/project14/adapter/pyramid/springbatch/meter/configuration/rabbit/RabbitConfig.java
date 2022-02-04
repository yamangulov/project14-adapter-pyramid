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

@Slf4j
@EnableRabbit
@Configuration
@RefreshScope
public class RabbitConfig {
    @Value("${rabbitmq.MetersUuids.exchange}")
    private String metersUuidsExchange;

    @Value("${rabbitmq.MetersUuids.routingKey}")
    private String metersUuidsRoutingKey;

    @Value("${rabbitmq.commands.queue}")
    private String defaultQueue;

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

    @Bean("rabbitTemplate")
    @RefreshScope
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

    @Bean
    @RefreshScope
    public AmqpAdmin amqpAdmin() {
        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory());
        if (!inputCommandQueue().getName().equals("PYRAMID_METERS.COMMANDS")) {
            log.error("Очередь {} имеет некорректное имя, проверьте сервер RabbitMQ, при полном перезапуске " +
                    "адаптера Пирамиды будет создана паразитная очередь с таким именем, и ее нужно удалить " +
                    "и создать очередь с правильным именем", inputCommandQueue());
        }
        return rabbitAdmin;
    }

    @Bean
    @RefreshScope
    public DirectExchange commandExchange() {
        return new DirectExchange(metersUuidsExchange);
    }

    @Bean("inputCommandQueue")
    @RefreshScope
    public Queue inputCommandQueue() {
        return new Queue(defaultQueue);
    }

    @Bean
    @RefreshScope
    public Binding commandBinding() {
        return BindingBuilder.bind(inputCommandQueue()).to(commandExchange()).with(metersUuidsRoutingKey);
    }

}
