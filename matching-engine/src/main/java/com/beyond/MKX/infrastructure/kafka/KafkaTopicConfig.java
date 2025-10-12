package com.beyond.MKX.infrastructure.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 카프카 토픽 자동 생성 설정.
 *
 * 목적
 * - 애플리케이션 기동 시 필수 토픽(executions, order-status, order-errors)을 보장 생성
 * - 브로커에 토픽이 없어서 발생하는 런타임 실패를 사전에 방지
 *
 * 비고
 * - PARTITIONS/REPLICA는 개발·테스트 기준 값이며, 운영 환경에서는 브로커/워크로드에 맞게 조정 필요
 */
@Configuration
public class KafkaTopicConfig {
    /** 기본 파티션 수(동시 처리량/키 분산 고려) */
    private static final int PARTITIONS = 4;
    /** 복제 계수(단일 브로커 환경 가정). 운영에서는 2~3 이상으로 설정 할 예정 */
    private static final short REPLICA  = 1;

    /** 체결 이벤트 토픽 */
    @Bean public NewTopic executionsTopic()   { return new NewTopic("executions",   PARTITIONS, REPLICA); }
    /** 주문 상태 이벤트 토픽 */
    @Bean public NewTopic orderStatusTopic()  { return new NewTopic("order-status", PARTITIONS, REPLICA); }
    /** 에러 알림(문자열) 토픽 */
    @Bean public NewTopic orderErrorsTopic()  { return new NewTopic("order-errors", PARTITIONS, REPLICA); }
}