package org.satel.eip.project14.adapter.pyramid.metrics.accumulator.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "metrics")
@Data
public class Metrics {
    String project;
    List<Channel> channels;

    @Data
    public static class Channel {
        String channel;
        String ruleName;
        List<Integer> interval;
    }
}
