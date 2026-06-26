package com.beyond.MKX.common.kafka.failure;

public enum KafkaFailureErrorType {
    ORDER_NOT_FOUND,
    ASSET_MISMATCH,
    DB_TIMEOUT,
    LOCK_TIMEOUT,
    JSON_PARSE_ERROR,
    MISSING_REQUIRED_FIELD,
    UNKNOWN
}
