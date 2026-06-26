package com.beyond.MKX.common.kafka.failure;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class KafkaFailureLogDTO {
    private UUID id;
    private String sourceTopic;
    private Integer sourcePartition;
    private Long sourceOffset;
    private String dlqTopic;
    private Integer dlqPartition;
    private Long dlqOffset;
    private String messageKey;
    private String payload;
    private String eventType;
    private KafkaFailureErrorType errorType;
    private String errorMessage;
    private String exceptionClass;
    private KafkaFailureStatus status;
    private int retryCount;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static KafkaFailureLogDTO from(KafkaFailureLog log) {
        return KafkaFailureLogDTO.builder()
                .id(log.getId())
                .sourceTopic(log.getSourceTopic())
                .sourcePartition(log.getSourcePartition())
                .sourceOffset(log.getSourceOffset())
                .dlqTopic(log.getDlqTopic())
                .dlqPartition(log.getDlqPartition())
                .dlqOffset(log.getDlqOffset())
                .messageKey(log.getMessageKey())
                .payload(log.getPayload())
                .eventType(log.getEventType())
                .errorType(log.getErrorType())
                .errorMessage(log.getErrorMessage())
                .exceptionClass(log.getExceptionClass())
                .status(log.getStatus())
                .retryCount(log.getRetryCount())
                .resolvedBy(log.getResolvedBy())
                .resolvedAt(log.getResolvedAt())
                .createdAt(log.getCreatedAt())
                .updatedAt(log.getUpdatedAt())
                .build();
    }
}
