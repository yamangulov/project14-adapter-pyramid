package org.satel.eip.project14.adapter.pyramid.metrics.accumulator;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.domain.generic.exception.GenericException;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity.AvailableMetrics;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity.Metrics;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.exception.AccumulatorRepositoryPostConstructException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.DoubleAccumulator;
import java.util.function.Supplier;

@Slf4j
@Component
@EnableScheduling
public class AccumulatorRepository {

    private final MeterRegistry meterRegistry;
    private final Metrics metrics;

    @Autowired
    public AccumulatorRepository(MeterRegistry meterRegistry, Metrics metrics) {
        this.meterRegistry = meterRegistry;
        this.metrics = metrics;
    }

    private static final Map<String, Map<Integer, GaugeToAccumulator>> INTERVAL_TO_GAUGE_HOLDER_MAP = new ConcurrentHashMap<>();

    private static final Map<String, DoubleAccumulator> DOUBLE_ACCUMULATOR_MAP = new HashMap<>();

    @PostConstruct
    public void init() {
        try {
            if (metrics == null  || metrics.getChannels().isEmpty() || metrics.getProject() == null || metrics.getProject().isBlank() ) {
                log.error("Некорректный конфигурационный yml файл, проверьте metrics.project и metric.channels",
                        new AccumulatorRepositoryPostConstructException("Некорректный конфигурационный yml файл, проверьте metrics.project и metric.channels"));
                return;
            }
            for (Metrics.Channel channel : metrics.getChannels()) {
                for (AvailableMetrics metric : AvailableMetrics.values()) {
                    Map<Integer, GaugeToAccumulator> intervalMap = new HashMap<>();
                    for (Integer interval : channel.getInterval()) {
                        DoubleAccumulator doubleAccumulator = new DoubleAccumulator(Double::sum, 0L);
                        String metricNameOrig = metrics.getProject().concat("metric.").concat(channel.getChannel()).concat(metric.getMetric()).concat(".").concat(interval.toString());
                        String metricNameShort = metrics.getProject()/*.concat("metric.").concat(channel.getChannel())*/.concat(metric.getMetric()).concat(".")/*.concat(interval.toString())*/;
                        GaugeToAccumulator gaugeToAccumulator = new GaugeToAccumulator(interval);
                        gaugeToAccumulator.setAccumulator(doubleAccumulator);
                        gaugeToAccumulator.setGauge(Gauge.builder(metricNameShort, AccumulatorRepository.value(metricNameOrig))
                                .description(metric.getDescription())
                                .tag("flow", channel.getChannel())
                                .tag("interval", interval.toString())
                                .register(meterRegistry));
                        DOUBLE_ACCUMULATOR_MAP.put(metricNameOrig, doubleAccumulator);
                        intervalMap.put(interval, gaugeToAccumulator);
                    }
                    INTERVAL_TO_GAUGE_HOLDER_MAP.put(channel.getChannel().concat(metric.getMetric()), intervalMap);

                }

            }

        } catch (Throwable e) {
            log.error(e.getMessage());
        }



    }

    public static Supplier<Number> value(String s) {
        return () ->  DOUBLE_ACCUMULATOR_MAP.get(s).get();
    }

    public Map<Integer, GaugeToAccumulator> getGaugeByMetric(String metric) {
        return INTERVAL_TO_GAUGE_HOLDER_MAP.get(metric);
    }

    @Scheduled(fixedDelay = 1000)
    public void checkTtl() throws GenericException {
        for (Map.Entry<String, Map<Integer, GaugeToAccumulator>> entry : INTERVAL_TO_GAUGE_HOLDER_MAP.entrySet()) {
            Map<Integer, GaugeToAccumulator> integerGaugeToAccumulatorMap = entry.getValue();
            for (Map.Entry<Integer, GaugeToAccumulator> e : integerGaugeToAccumulatorMap.entrySet()) {
                e.getValue().scheduleCheck();
            }
        }
    }



}
