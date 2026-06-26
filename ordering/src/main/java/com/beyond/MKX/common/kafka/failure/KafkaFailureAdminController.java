package com.beyond.MKX.common.kafka.failure;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/kafka-failures")
@RequiredArgsConstructor
public class KafkaFailureAdminController {

    private final KafkaFailureLogService service;

    @GetMapping
    public ResponseEntity<?> findFailures(
            @RequestParam(required = false) KafkaFailureStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.ok(service.findFailures(status, PageRequest.of(page, size)), "Kafka 실패 이력 조회 성공");
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getFailure(@PathVariable UUID id) {
        return ApiResponse.ok(service.getFailure(id), "Kafka 실패 이력 상세 조회 성공");
    }

    @PostMapping("/{id}/reprocess")
    public ResponseEntity<?> reprocess(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String adminId
    ) {
        return ApiResponse.ok(service.reprocess(id, adminId), "Kafka 실패 이력 재처리 완료");
    }

    @PostMapping("/{id}/discard")
    public ResponseEntity<?> discard(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String adminId
    ) {
        return ApiResponse.ok(service.discard(id, adminId), "Kafka 실패 이력 폐기 완료");
    }
}
