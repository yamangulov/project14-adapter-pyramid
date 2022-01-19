package org.satel.eip.project14.adapter.pyramid.endpoint;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component
@Endpoint(id = "rabbitmq")
public class RabbitMQActuatorEndpoint {

    private final Counter outCounter;
    private final Counter inCounter;

    public RabbitMQActuatorEndpoint(@Qualifier("outCounter") Counter outCounter, @Qualifier("inCounter") Counter inCounter) {
        this.outCounter = outCounter;
        this.inCounter = inCounter;
    }

    @ReadOperation
    public Map<String, Double> getRabbitMQCounters() {
        Map<String, Double> counters = new ConcurrentHashMap<>();
        counters.put("outcome_rabbitmq_package", outCounter != null ? outCounter.count() : 0);
        counters.put("income_rabbitmq_package", inCounter != null ? inCounter.count() : 0);
        return counters;
    }

}
