package org.satel.eip.project14.adapter.pyramid.endpoint;

import io.micrometer.core.instrument.Counter;
import org.satel.eip.project14.adapter.pyramid.wrapper.DoubleWrapper;
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
    private final DoubleWrapper inGaugeCounter;
    private final DoubleWrapper outGaugeCounter;

    public RabbitMQActuatorEndpoint(@Qualifier("outCounter") Counter outCounter, @Qualifier("inCounter") Counter inCounter, @Qualifier("inGaugeCounter") DoubleWrapper inGaugeCounter, @Qualifier("outGaugeCounter") DoubleWrapper outGaugeCounter) {
        this.outCounter = outCounter;
        this.inCounter = inCounter;
        this.inGaugeCounter = inGaugeCounter;
        this.outGaugeCounter = outGaugeCounter;
    }

    @ReadOperation
    public Map<String, Double> getRabbitMQCounters() {
        Map<String, Double> counters = new ConcurrentHashMap<>();
        counters.put("outcome_rabbitmq_package", outCounter != null ? outCounter.count() : 0);
        counters.put("income_rabbitmq_package", inCounter != null ? inCounter.count() : 0);
        counters.put("outcome_rabbitmq_package_resetable", outGaugeCounter.getValue());
        counters.put("income_rabbitmq_package_resetable", inGaugeCounter.getValue());
        return counters;
    }

}
