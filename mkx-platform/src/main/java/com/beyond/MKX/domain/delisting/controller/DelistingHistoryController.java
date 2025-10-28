package com.beyond.MKX.domain.delisting.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.delisting.entity.DelistingHistory;
import com.beyond.MKX.domain.delisting.repository.DelistingHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/delisting/history")
@RequiredArgsConstructor
public class DelistingHistoryController {

    private final DelistingHistoryRepository historyRepository;

    /**
     * 주식별 상장폐지 이력 조회
     */
    @GetMapping("/stock/{stockId}")
    public ResponseEntity<?> getHistoryByStock(@PathVariable UUID stockId) {
        List<DelistingHistory> history = historyRepository.findByStockId(stockId);
        return ApiResponse.ok(history, "주식별 상장폐지 이력 조회 성공");
    }

    /**
     * 액션 유형별 이력 조회
     */
    @GetMapping("/action-type/{actionType}")
    public ResponseEntity<?> getHistoryByActionType(@PathVariable com.beyond.MKX.domain.delisting.entity.ActionType actionType) {
        List<DelistingHistory> history = historyRepository.findByActionType(actionType);
        return ApiResponse.ok(history, "액션 유형별 이력 조회 성공");
    }

    /**
     * 실행자별 이력 조회
     */
    @GetMapping("/executed-by/{executedBy}")
    public ResponseEntity<?> getHistoryByExecutedBy(@PathVariable UUID executedBy) {
        List<DelistingHistory> history = historyRepository.findByExecutedBy(executedBy);
        return ApiResponse.ok(history, "실행자별 이력 조회 성공");
    }
}

