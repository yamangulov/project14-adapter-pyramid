package org.satel.eip.project14.adapter.pyramid.metrics.accumulator;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity.AvailableMetrics;
import org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity.Metrics;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
@Slf4j
public class AccumulatorService {

    private final AccumulatorRepository accumulatorRepository;
    private final Metrics metrics;

    public void increment(Metrics.Channel channel, AvailableMetrics availableMetrics) {
        Map<Integer, GaugeToAccumulator> intervalMap = accumulatorRepository.getGaugeByMetric(channel.getChannel().concat(availableMetrics.getMetric()));
        intervalMap.forEach((s, gaugeToAccumulator) ->  gaugeToAccumulator.increment());
    }

    public Metrics.Channel getChannel(String channelName) {
        for (Metrics.Channel channel : metrics.getChannels()) {
            if (channel.getChannel().equals(channelName)) {
                return channel;
            }
        }
        return null;
    }

}
