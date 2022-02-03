package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.refresh;

import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.PyramidApplication;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Slf4j
@Component
public class RefreshListener {
    ConfigurableApplicationContext applicationContext;
//    RabbitListenerEndpointRegistry rabbitListenerEndpointRegistry;

    @Autowired
    public RefreshListener(ConfigurableApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

//    @PostConstruct
//    public void init() {
//        this.rabbitListenerEndpointRegistry = (RabbitListenerEndpointRegistry) applicationContext.getBean("org.springframework.amqp.rabbit.config.internalRabbitListenerEndpointRegistry");
//    }


    @EventListener
    public void onEnvironmentChangeEvent(EnvironmentChangeEvent event) {
//        MessageListenerContainer listenerContainer = rabbitListenerEndpointRegistry.getListenerContainer("pyramidCommandListener");
//        listenerContainer.stop();
//        listenerContainer.start();
//        log.info("listenerContainer restarted");
        PyramidApplication.restart();
    }
}
