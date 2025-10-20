package com.beyond.MKX.common.util;

import com.influxdb.client.InfluxDBClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * InfluxDB 연결 상태 확인
 * 
 * 애플리케이션 시작 시 InfluxDB 연결을 테스트하고 로그를 출력
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InfluxDBHealthCheck {

    private final InfluxDBClient influxDBClient;

    @Value("${influx.url}")
    private String url;

    @Value("${influx.org}")
    private String organization;

    @Value("${influx.bucket}")
    private String bucket;

    /**
     * 애플리케이션 시작 후 InfluxDB 연결 테스트
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkInfluxDBConnection() {
        try {
            log.info("========================================");
            log.info("InfluxDB Connection Test Started");
            log.info("========================================");
            log.info("URL: {}", url);
            log.info("Organization: {}", organization);
            log.info("Bucket: {}", bucket);
            
            // Ping 테스트
            boolean isConnected = influxDBClient.ping();
            
            if (isConnected) {
                log.info("✅ InfluxDB Connection: SUCCESS");
                log.info("========================================");
            } else {
                log.error("❌ InfluxDB Connection: FAILED - Ping returned false");
                log.error("========================================");
                printTroubleshootingGuide();
            }
            
        } catch (Exception e) {
            log.error("❌ InfluxDB Connection: FAILED");
            log.error("Error: {}", e.getMessage());
            log.error("========================================");
            printTroubleshootingGuide();
        }
    }

    /**
     * 문제 해결 가이드 출력
     */
    private void printTroubleshootingGuide() {
        log.error("");
        log.error("📋 InfluxDB 연결 실패 - 문제 해결 가이드:");
        log.error("");
        log.error("1. InfluxDB가 실행 중인지 확인:");
        log.error("   $ ps aux | grep influxd");
        log.error("   또는");
        log.error("   $ influxd");
        log.error("");
        log.error("2. InfluxDB UI 접속 확인:");
        log.error("   브라우저에서 http://localhost:8086 접속");
        log.error("");
        log.error("3. Organization 확인:");
        log.error("   - InfluxDB UI 좌측 하단에서 Organization 이름 확인");
        log.error("   - application.yml의 influx.org 값과 일치해야 함");
        log.error("");
        log.error("4. Bucket 생성:");
        log.error("   - Load Data → Buckets 메뉴");
        log.error("   - Create Bucket 클릭");
        log.error("   - Name: MKX_MarketData");
        log.error("");
        log.error("5. API Token 생성:");
        log.error("   - Load Data → API Tokens 메뉴");
        log.error("   - Generate API Token → All Access API Token");
        log.error("   - 생성된 Token을 application.yml의 influx.token에 설정");
        log.error("");
        log.error("6. application.yml 설정 확인:");
        log.error("   influx:");
        log.error("     url: http://127.0.0.1:8086");
        log.error("     token: <생성한_토큰>");
        log.error("     org: <Organization_이름>");
        log.error("     bucket: MKX_MarketData");
        log.error("");
        log.error("========================================");
    }
}
