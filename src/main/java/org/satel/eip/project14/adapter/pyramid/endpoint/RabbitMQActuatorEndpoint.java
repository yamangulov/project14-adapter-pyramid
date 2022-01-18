package org.satel.eip.project14.adapter.pyramid.endpoint;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Endpoint(id = "rabbitmq")
public class RabbitMQActuatorEndpoint {

    private final Counter counter;
    private final Counter inCounter;

    @Autowired
    public RabbitMQActuatorEndpoint(Counter counter, Counter inCounter) {
        this.counter = Metrics.globalRegistry
                .find("outcome_rabbitmq_package").counter();
        this.inCounter = Metrics.globalRegistry
                .find("income_rabbitmq_package").counter();;
    }

    @ReadOperation
    public Map<String, Double> getRabbitMQCounters() {
        Map<String, Double> counters = new ConcurrentHashMap<>();
        counters.put("outcome_rabbitmq_package", counter != null ? counter.count() : 0);
        counters.put("income_rabbitmq_package", inCounter != null ? inCounter.count() : 0);
        return counters;
    }

}
