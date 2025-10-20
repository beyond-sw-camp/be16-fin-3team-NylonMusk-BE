package com.beyond.MKX.common.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * InfluxDB 클라이언트 설정
 * 
 * 체결 데이터를 시계열 데이터베이스인 InfluxDB에 저장하기 위한 클라이언트 설정
 */
@Configuration
@Slf4j
public class InfluxDBConfig {

    @Value("${influx.url}")
    private String url;

    @Value("${influx.token}")
    private String token;

    @Value("${influx.org}")
    private String organization;

    @Value("${influx.bucket}")
    private String bucket;

    @Bean
    public InfluxDBClient influxDBClient() {
        log.info("Initializing InfluxDB Client - URL: {}, Org: {}, Bucket: {}", url, organization, bucket);
        return InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket);
    }

    @Bean
    public String influxBucket() {
        return bucket;
    }

    @Bean
    public String influxOrg() {
        return organization;
    }
}
