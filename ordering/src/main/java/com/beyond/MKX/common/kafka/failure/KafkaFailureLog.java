package com.beyond.MKX.common.kafka.failure;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "kafka_failure_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_kafka_failure_source_record",
                        columnNames = {"source_topic", "source_partition", "source_offset"}
                )
        }
)
public class KafkaFailureLog extends BaseIdAndTimeEntity {

    @Column(name = "source_topic", nullable = false)
    private String sourceTopic;

    @Column(name = "source_partition", nullable = false)
    private Integer sourcePartition;

    @Column(name = "source_offset", nullable = false)
    private Long sourceOffset;

    @Column(name = "dlq_topic", nullable = false)
    private String dlqTopic;

    @Column(name = "dlq_partition", nullable = false)
    private Integer dlqPartition;

    @Column(name = "dlq_offset", nullable = false)
    private Long dlqOffset;

    @Column(name = "message_key")
    private String messageKey;

    @Column(columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private KafkaFailureErrorType errorType;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private String exceptionClass;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private KafkaFailureStatus status;

    @Column(nullable = false)
    private int retryCount;

    private String resolvedBy;

    private LocalDateTime resolvedAt;

    public void markReprocessed(String resolvedBy) {
        this.status = KafkaFailureStatus.REPROCESSED;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markDiscarded(String resolvedBy) {
        this.status = KafkaFailureStatus.DISCARDED;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = LocalDateTime.now();
    }

    public void markFailedReprocess(Throwable throwable) {
        this.status = KafkaFailureStatus.FAILED_REPROCESS;
        this.retryCount++;
        this.exceptionClass = throwable.getClass().getName();
        this.errorMessage = throwable.getMessage();
    }
}
