package com.beyond.MKX.common.redis.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.redis.common")
@Getter
@Setter
public class CommonRedisProps {
    private String host;
    private int port;
}
