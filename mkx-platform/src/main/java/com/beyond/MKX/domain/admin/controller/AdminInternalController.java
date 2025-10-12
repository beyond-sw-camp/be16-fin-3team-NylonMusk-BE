package com.beyond.MKX.domain.admin.controller;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * 내부 전용 Admin 조회 API
 * - 주문서비스 등 내부 시스템이 관리자 소속 증권사 ID를 조회할 때 사용
 */
@RestController
@RequestMapping("/api/internal/admins")
@RequiredArgsConstructor
public class AdminInternalController {

    private final AdminRepository adminRepository;

    @GetMapping("/{adminId}/brokerage-id")
    public ResponseEntity<?> getBrokerageId(@PathVariable UUID adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        if (admin.getSecuritiesFirm() == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "증권사 소속 아님"));
        }
        return ResponseEntity.ok(Map.of("brokerageId", admin.getSecuritiesFirm().getId()));
    }
}

