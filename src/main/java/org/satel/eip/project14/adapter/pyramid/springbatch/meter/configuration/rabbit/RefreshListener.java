package org.satel.eip.project14.adapter.pyramid.springbatch.meter.configuration.rabbit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.scope.refresh.RefreshScopeRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RefreshListener {

    private final SimpleMessageListenerContainer simpleMessageListenerContainer;

    @Autowired
    public RefreshListener(SimpleMessageListenerContainer simpleMessageListenerContainer) {
        this.simpleMessageListenerContainer = simpleMessageListenerContainer;
    }

    // необходимо перезапустить контейнер слушателей после обновления конфигурации
    @EventListener
    public void onEnvironmentChangeEvent(RefreshScopeRefreshedEvent event) {
        simpleMessageListenerContainer.stop();
        simpleMessageListenerContainer.start();
    }
}
