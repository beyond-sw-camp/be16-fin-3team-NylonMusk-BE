package com.beyond.MKX.domain.delisting.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.delisting.entity.DelistingCompensation;
import com.beyond.MKX.domain.delisting.repository.DelistingCompensationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delisting/compensation")
@RequiredArgsConstructor
public class DelistingCompensationController {

    private final DelistingCompensationRepository compensationRepository;

    /**
     * 주식별 보상 목록 조회
     */
    @GetMapping("/stock/{stockId}")
    public ResponseEntity<?> getCompensationByStock(@PathVariable UUID stockId, 
                                                    @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted) {
        List<DelistingCompensation> compensation;
        if (includeDeleted) {
            compensation = compensationRepository.findAllByStockId(stockId);
        } else {
            compensation = compensationRepository.findByStockId(stockId);
        }
        return ApiResponse.ok(compensation, "주식별 보상 목록 조회 성공");
    }

    /**
     * 회원별 보상 목록 조회
     */
    @GetMapping("/member/{memberAccountId}")
    public ResponseEntity<?> getCompensationByMember(@PathVariable UUID memberAccountId) {
        List<DelistingCompensation> compensation = compensationRepository.findByMemberAccountId(memberAccountId);
        return ApiResponse.ok(compensation, "회원별 보상 목록 조회 성공");
    }

    /**
     * 상태별 보상 목록 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getCompensationByStatus(@PathVariable com.beyond.MKX.domain.delisting.entity.CompensationStatus status) {
        List<DelistingCompensation> compensation = compensationRepository.findByStatus(status);
        return ApiResponse.ok(compensation, "상태별 보상 목록 조회 성공");
    }

    /**
     * 대기 중인 보상 목록 조회
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingCompensations() {
        List<DelistingCompensation> compensation = compensationRepository.findPendingCompensations();
        return ApiResponse.ok(compensation, "대기 중인 보상 목록 조회 성공");
    }
}

