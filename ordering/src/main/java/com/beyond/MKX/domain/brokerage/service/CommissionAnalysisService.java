package com.beyond.MKX.domain.brokerage.service;

import com.beyond.MKX.domain.brokerage.dto.DailyCommissionDTO;
import com.beyond.MKX.domain.brokerage.dto.HourlyCommissionTrendDTO;
import com.beyond.MKX.domain.brokerage.dto.PeriodCommissionDTO;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import com.beyond.MKX.domain.execution.repository.LedgerRepository;
import com.beyond.MKX.domain.order.repository.OrderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 수수료 분석 서비스
 * BrokerageDashboardService에 이 메서드들을 추가하면 됩니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommissionAnalysisService {

    private final LedgerRepository ledgerRepository;
    private final OrderLogRepository orderLogRepository;

    /**
     * 일일 수수료 합산 조회
     * 
     * @param brokerageId 증권사 ID
     * @param date 조회 날짜 (null이면 오늘)
     * @return 일일 수수료 데이터
     */
    @Transactional(readOnly = true)
    public DailyCommissionDTO getDailyCommission(UUID brokerageId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        
        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);
        
        // 총 수수료
        Long totalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
            brokerageId, startDateTime, endDateTime);
        
        // 매수 수수료
        Long buyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.BUY, startDateTime, endDateTime);
        
        // 매도 수수료
        Long sellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.SELL, startDateTime, endDateTime);
        
        // 거래 건수
        Long tradeCount = ledgerRepository.getTradeCountByBrokerageIdAndDateRange(
            brokerageId, startDateTime, endDateTime);
        
        // 활성 거래자 수
        Long activeTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
            brokerageId, startDateTime, endDateTime);
        
        return DailyCommissionDTO.builder()
            .date(date)
            .totalCommission(totalCommission != null ? totalCommission : 0L)
            .buyCommission(buyCommission != null ? buyCommission : 0L)
            .sellCommission(sellCommission != null ? sellCommission : 0L)
            .tradeCount(tradeCount != null ? tradeCount.intValue() : 0)
            .activeTraders(activeTraders != null ? activeTraders.intValue() : 0)
            .build();
    }

    /**
     * 기간별 수수료 조회 (7일, 30일, 90일, 365일)
     * 
     * @param brokerageId 증권사 ID
     * @param days 기간 (7, 30, 90, 365)
     * @return 기간별 수수료 데이터
     */
    @Transactional(readOnly = true)
    public PeriodCommissionDTO getCommissionByPeriod(UUID brokerageId, Integer days) {
        if (days == null) {
            days = 7;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minusDays(days);
        LocalDateTime previousPeriodStart = now.minusDays(days * 2);
        
        // 현재 기간 수수료
        Long totalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
            brokerageId, periodStart, now);
        
        // 매수 수수료
        Long buyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.BUY, periodStart, now);
        
        // 매도 수수료
        Long sellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.SELL, periodStart, now);
        
        // 이전 기간 수수료 (변화율 계산용)
        Long previousCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
            brokerageId, previousPeriodStart, periodStart);
        
        // 변화율 계산
        double changePercent = 0.0;
        if (previousCommission != null && previousCommission > 0) {
            changePercent = ((double)(totalCommission - previousCommission) / previousCommission) * 100.0;
        } else if (totalCommission != null && totalCommission > 0) {
            changePercent = 100.0;
        }
        
        // 일별 상세 데이터 생성
        List<DailyCommissionDTO> dailyBreakdown = generateDailyBreakdown(brokerageId, periodStart, now);
        
        // 일평균 계산
        double dailyAverage = totalCommission != null ? (double) totalCommission / days : 0.0;
        
        return PeriodCommissionDTO.builder()
            .periodDays(days)
            .totalCommission(totalCommission != null ? totalCommission : 0L)
            .buyCommission(buyCommission != null ? buyCommission : 0L)
            .sellCommission(sellCommission != null ? sellCommission : 0L)
            .dailyAverage(dailyAverage)
            .changePercent(changePercent)
            .dailyBreakdown(dailyBreakdown)
            .build();
    }

    /**
     * 시간대별 수익 추이 조회
     * 
     * @param brokerageId 증권사 ID
     * @param days 기간 (기본값: 7일)
     * @return 시간대별 수수료 추이 리스트
     */
    @Transactional(readOnly = true)
    public List<HourlyCommissionTrendDTO> getHourlyCommissionTrends(UUID brokerageId, Integer days) {
        if (days == null) {
            days = 7;
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = now.minusDays(days);
        
        // 시간대별 집계 데이터 조회
        List<Object[]> hourlyData = ledgerRepository.getHourlyCommissionTrends(
            brokerageId, startDateTime, now);
        
        List<HourlyCommissionTrendDTO> trends = new ArrayList<>();
        for (Object[] row : hourlyData) {
            Integer hour = (Integer) row[0];
            Long totalCommission = (Long) row[1];
            Long buyCommission = (Long) row[2];
            Long sellCommission = (Long) row[3];
            Long tradeCount = (Long) row[4];
            
            // 해당 시간대의 활성 거래자 수 계산
            // 전체 기간에서 해당 시간대에 주문한 고유 계좌 수
            LocalDateTime hourStart = startDateTime.withHour(hour).withMinute(0).withSecond(0);
            LocalDateTime hourEnd = hourStart.plusHours(1);
            Long activeTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                brokerageId, hourStart, hourEnd);
            
            trends.add(HourlyCommissionTrendDTO.builder()
                .hour(hour)
                .totalCommission(totalCommission)
                .buyCommission(buyCommission)
                .sellCommission(sellCommission)
                .tradeCount(tradeCount.intValue())
                .activeTraders(activeTraders != null ? activeTraders.intValue() : 0)
                .build());
        }
        
        return trends;
    }

    /**
     * 일별 수수료 상세 데이터 생성 (헬퍼 메서드)
     */
    private List<DailyCommissionDTO> generateDailyBreakdown(
        UUID brokerageId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        
        // 일별 집계 데이터 조회
        List<Object[]> dailyData = ledgerRepository.getDailyCommissionBreakdown(
            brokerageId, startDateTime, endDateTime);
        
        List<DailyCommissionDTO> breakdown = new ArrayList<>();
        for (Object[] row : dailyData) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate date = sqlDate.toLocalDate();
            Long totalCommission = (Long) row[1];
            Long buyCommission = (Long) row[2];
            Long sellCommission = (Long) row[3];
            Long tradeCount = (Long) row[4];
            
            // 해당 날짜의 활성 거래자 수 계산
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            Long activeTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                brokerageId, dayStart, dayEnd);
            
            breakdown.add(DailyCommissionDTO.builder()
                .date(date)
                .totalCommission(totalCommission)
                .buyCommission(buyCommission)
                .sellCommission(sellCommission)
                .tradeCount(tradeCount.intValue())
                .activeTraders(activeTraders != null ? activeTraders.intValue() : 0)
                .build());
        }
        
        return breakdown;
    }
}
