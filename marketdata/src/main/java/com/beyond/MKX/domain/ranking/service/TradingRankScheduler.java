package com.beyond.MKX.domain.ranking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 거래대금/거래량 랭킹 스케줄러
 *
 * 5초마다 24시간 거래대금/거래량을 집계하여 Redis에 랭킹 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradingRankScheduler {

    private final TradingRankService tradingRankService;

    /**
     * 거래대금/거래량 랭킹 업데이트 스케줄러
     *
     * 실행 주기: 5초마다
     * 집계 기간: 현재 시각 기준 24시간 전 데이터
     */
    @Scheduled(fixedRate = 5000) // 5초 (5000ms)
    public void updateTradingRanks() {
        try {
            System.out.println("[SCHEDULER] 거래대금/거래량 랭킹 업데이트 시작");
            log.debug("[SCHEDULER] 거래대금/거래량 랭킹 업데이트 시작");

            tradingRankService.updateTradingRanks();

            log.debug("[SCHEDULER] 거래대금/거래량 랭킹 업데이트 완료");
            System.out.println("[SCHEDULER] 거래대금/거래량 랭킹 업데이트 완료");

        } catch (Exception e) {
            log.error("[SCHEDULER] 거래대금/거래량 랭킹 업데이트 실패", e);
        }
    }

    /**
     * 스케줄러 정보 로깅 (1분마다)
     *
     * 현재 스케줄러 동작 상태 확인용
     */
    @Scheduled(fixedRate = 60000) // 1분 (60000ms)
    public void logSchedulerStatus() {
        log.info("[SCHEDULER] 거래대금/거래량 랭킹 스케줄러 동작 중 - 업데이트 주기: 5초");
    }
}


