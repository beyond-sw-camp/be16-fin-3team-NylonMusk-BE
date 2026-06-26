package com.beyond.MKX.common.kafka.failure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KafkaFailureLogService {

    private final KafkaFailureLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveDlqRecord(ConsumerRecord<?, ?> record) {
        String sourceTopic = originalTopic(record);
        Integer sourcePartition = originalPartition(record);
        Long sourceOffset = originalOffset(record);

        if (repository.existsBySourceTopicAndSourcePartitionAndSourceOffset(
                sourceTopic, sourcePartition, sourceOffset)) {
            return;
        }

        String exceptionClass = headerAsString(record, KafkaHeaders.DLT_EXCEPTION_FQCN);
        String errorMessage = headerAsString(record, KafkaHeaders.DLT_EXCEPTION_MESSAGE);

        repository.save(KafkaFailureLog.builder()
                .sourceTopic(sourceTopic)
                .sourcePartition(sourcePartition)
                .sourceOffset(sourceOffset)
                .dlqTopic(record.topic())
                .dlqPartition(record.partition())
                .dlqOffset(record.offset())
                .messageKey(record.key() == null ? null : String.valueOf(record.key()))
                .payload(toJson(record.value()))
                .eventType(resolveEventType(sourceTopic, record.value()))
                .errorType(resolveErrorType(exceptionClass, errorMessage))
                .errorMessage(errorMessage)
                .exceptionClass(exceptionClass)
                .status(KafkaFailureStatus.PENDING)
                .retryCount(0)
                .build());
    }

    @Transactional(readOnly = true)
    public Page<KafkaFailureLogDTO> findFailures(KafkaFailureStatus status, Pageable pageable) {
        Page<KafkaFailureLog> logs = status == null
                ? repository.findAll(pageable)
                : repository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return logs.map(KafkaFailureLogDTO::from);
    }

    @Transactional(readOnly = true)
    public KafkaFailureLogDTO getFailure(UUID id) {
        return repository.findById(id)
                .map(KafkaFailureLogDTO::from)
                .orElseThrow(() -> new EntityNotFoundException("Kafka 실패 이력을 찾을 수 없습니다."));
    }

    @Transactional
    public KafkaFailureLogDTO reprocess(UUID id, String resolvedBy) {
        throw new UnsupportedOperationException("Kafka 실패 이력 재처리는 원본 토픽 재발행 방식으로 별도 구현이 필요합니다.");
    }

    @Transactional
    public KafkaFailureLogDTO discard(UUID id, String resolvedBy) {
        KafkaFailureLog log = repository.findByIdAndStatus(id, KafkaFailureStatus.PENDING)
                .orElseThrow(() -> new EntityNotFoundException("폐기 가능한 Kafka 실패 이력을 찾을 수 없습니다."));
        log.markDiscarded(resolvedBy);
        return KafkaFailureLogDTO.from(log);
    }

    private String originalTopic(ConsumerRecord<?, ?> record) {
        String originalTopic = headerAsString(record, KafkaHeaders.DLT_ORIGINAL_TOPIC);
        if (originalTopic != null && !originalTopic.isBlank()) {
            return originalTopic;
        }
        return record.topic().endsWith(".DLQ")
                ? record.topic().substring(0, record.topic().length() - 4)
                : record.topic();
    }

    private Integer originalPartition(ConsumerRecord<?, ?> record) {
        Integer originalPartition = headerAsInteger(record, KafkaHeaders.DLT_ORIGINAL_PARTITION);
        return originalPartition == null ? record.partition() : originalPartition;
    }

    private Long originalOffset(ConsumerRecord<?, ?> record) {
        Long originalOffset = headerAsLong(record, KafkaHeaders.DLT_ORIGINAL_OFFSET);
        return originalOffset == null ? record.offset() : originalOffset;
    }

    private String headerAsString(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private Integer headerAsInteger(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) {
            return null;
        }
        byte[] value = header.value();
        if (value.length == Integer.BYTES) {
            return ByteBuffer.wrap(value).getInt();
        }
        try {
            return Integer.parseInt(new String(value, StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long headerAsLong(ConsumerRecord<?, ?> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header == null || header.value() == null) {
            return null;
        }
        byte[] value = header.value();
        if (value.length == Long.BYTES) {
            return ByteBuffer.wrap(value).getLong();
        }
        try {
            return Long.parseLong(new String(value, StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private String resolveEventType(String sourceTopic, Object value) {
        if (value != null) {
            return value.getClass().getSimpleName();
        }
        return switch (sourceTopic) {
            case "executions" -> "ExecutionEvent";
            case "order-status" -> "OrderStatusEvent";
            default -> "Unknown";
        };
    }

    private KafkaFailureErrorType resolveErrorType(String exceptionClass, String errorMessage) {
        String className = exceptionClass == null ? "" : exceptionClass;
        String message = errorMessage == null ? "" : errorMessage;
        if (className.contains("Deserialization") || className.contains("Json") || message.contains("JSON")) {
            return KafkaFailureErrorType.JSON_PARSE_ERROR;
        }
        if (className.contains("SQLTimeout") || className.contains("QueryTimeout") || message.contains("timeout")) {
            return KafkaFailureErrorType.DB_TIMEOUT;
        }
        if (className.contains("Lock") || message.contains("lock")) {
            return KafkaFailureErrorType.LOCK_TIMEOUT;
        }
        if (message.contains("주문")) {
            return KafkaFailureErrorType.ORDER_NOT_FOUND;
        }
        if (message.contains("보유 주식") || message.contains("자산")) {
            return KafkaFailureErrorType.ASSET_MISMATCH;
        }
        if (className.contains("IllegalArgument") || className.contains("MethodArgument")) {
            return KafkaFailureErrorType.MISSING_REQUIRED_FIELD;
        }
        return KafkaFailureErrorType.UNKNOWN;
    }
}
