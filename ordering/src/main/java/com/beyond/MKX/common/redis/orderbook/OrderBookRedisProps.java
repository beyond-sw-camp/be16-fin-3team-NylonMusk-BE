package com.beyond.MKX.common.redis.orderbook;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "spring.redis.order-book")
@Getter
@Setter
public class OrderBookRedisProps {
    private Duration timeout = Duration.ofSeconds(3);
    private Cluster cluster = new Cluster();

    @Getter
    @Setter
    public static class Cluster {
        private List<String> nodes = List.of();
        private Integer maxRedirects;
    }
}
