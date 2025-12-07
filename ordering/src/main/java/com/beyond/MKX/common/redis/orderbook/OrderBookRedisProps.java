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

    // 공통
    private Duration timeout = Duration.ofSeconds(3);

    // 단일 Redis용
    private String host;
    private int port = 6379;           // 없으면 기본 값

    // Cluster 모드
    private Cluster cluster = new Cluster();

    @Getter
    @Setter
    public static class Cluster {
        private List<String> nodes = List.of();
        private Integer maxRedirects;
    }

    /**
     * 클러스터 모드 활성 여부 (편의 메서드)
     */
    public boolean isClusterEnabled() {
        return cluster != null
                && cluster.getNodes() != null
                && !cluster.getNodes().isEmpty();
    }
}
