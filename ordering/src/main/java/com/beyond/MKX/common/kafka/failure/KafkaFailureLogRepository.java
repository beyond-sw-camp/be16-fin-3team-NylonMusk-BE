package com.beyond.MKX.common.kafka.failure;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KafkaFailureLogRepository extends JpaRepository<KafkaFailureLog, UUID> {

    boolean existsBySourceTopicAndSourcePartitionAndSourceOffset(
            String sourceTopic,
            Integer sourcePartition,
            Long sourceOffset
    );

    Optional<KafkaFailureLog> findByIdAndStatus(UUID id, KafkaFailureStatus status);

    Page<KafkaFailureLog> findByStatusOrderByCreatedAtDesc(KafkaFailureStatus status, Pageable pageable);
}
