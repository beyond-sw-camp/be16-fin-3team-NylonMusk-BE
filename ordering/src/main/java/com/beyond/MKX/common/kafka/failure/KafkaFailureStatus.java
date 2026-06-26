package com.beyond.MKX.common.kafka.failure;

public enum KafkaFailureStatus {
    PENDING,
    REPROCESSED,
    DISCARDED,
    FAILED_REPROCESS
}
