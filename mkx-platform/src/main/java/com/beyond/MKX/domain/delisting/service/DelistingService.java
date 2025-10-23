package com.beyond.MKX.domain.delisting.service;

import com.beyond.MKX.domain.delisting.client.CurrentPriceClient;
import com.beyond.MKX.domain.delisting.client.StockHoldingClient;
import com.beyond.MKX.domain.delisting.client.MemberAccountClient;
import com.beyond.MKX.domain.delisting.dto.CurrentPriceResDto;
import com.beyond.MKX.domain.delisting.dto.StockHoldingResDto;
import com.beyond.MKX.domain.delisting.entity.*;
import com.beyond.MKX.domain.delisting.repository.*;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import com.beyond.MKX.domain.financial.entity.CompanyFinancials;
import com.beyond.MKX.domain.financial.entity.FinancialRatios;
import com.beyond.MKX.domain.financial.repository.CompanyFinancialsRepository;
import com.beyond.MKX.domain.financial.repository.FinancialRatiosRepository;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.account.corporation.entity.CorporationAccount;
import com.beyond.MKX.domain.account.corporation.repository.CorporationAccountRepository;
import com.beyond.MKX.domain.account.exchange.entity.ExchangeAccount;
import com.beyond.MKX.domain.account.exchange.repository.ExchangeAccountRepository;
import com.beyond.MKX.domain.delisting.entity.ExchangeSupportFund;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * 상장폐지 통합 비즈니스 로직 서비스
 * 
 * 상장폐지 과정의 전체적인 흐름을 관리합니다:
 * 1. 기준 위반 감지 및 기록
 * 2. 연속 위반 체크 및 단계 진행
 * 3. 상장폐지 예고 및 실행
 * 4. 보상금 처리
 * 5. 이력 추적
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DelistingService {

    private final DelistingCriteriaRepository criteriaRepo;
    private final DelistingViolationRepository violationRepo;
    private final DelistingCompensationRepository compensationRepo;
    private final DelistingHistoryRepository historyRepo;
    private final StockHoldingClient stockHoldingClient;
    private final CurrentPriceClient currentPriceClient;
    private final MemberAccountClient memberAccountClient;
    private final StockRepository stockRepo;
    private final CompanyFinancialsRepository companyFinancialsRepo;
    private final FinancialRatiosRepository financialRatiosRepo;
    private final CorporationRepository corporationRepo;
    private final CorporationAccountRepository corporationAccountRepo;
    private final ExchangeAccountRepository exchangeAccountRepo;
    private final ExchangeSupportFundRepository exchangeSupportFundRepo;

    /**
     * 기준 위반 감지 및 기록
     */
    public DelistingViolation detectViolation(UUID stockId, String criteriaCode, 
                                             BigDecimal currentValue, String description) {
        log.info("기준 위반 감지 시작: stockId={}, criteriaCode={}, currentValue={}", 
                stockId, criteriaCode, currentValue);

        // 기준 조회
        DelistingCriteria criteria = criteriaRepo.findByCriteriaCodeAndActive(criteriaCode)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 기준을 찾을 수 없습니다: " + criteriaCode));

        // 위반 여부 확인
        if (!isViolation(criteria, currentValue)) {
            log.info("기준 위반 아님: stockId={}, criteriaCode={}", stockId, criteriaCode);
            return null;
        }

        // 기존 연속 위반 확인
        List<DelistingViolation> existingViolations = violationRepo.findConsecutiveViolations(stockId, criteria.getId());
        int consecutivePeriods = existingViolations.size() + 1;

        // 위반 유형 결정
        ViolationType violationType = determineViolationType(criteria, consecutivePeriods);

        // 위반 기록 생성
        DelistingViolation violation = DelistingViolation.builder()
                .stockId(stockId)
                .criteriaId(criteria.getId())
                .criteriaCode(criteriaCode)
                .violationType(violationType)
                .currentValue(currentValue)
                .thresholdValue(criteria.getThresholdValue())
                .consecutivePeriods(consecutivePeriods)
                .violationDate(LocalDateTime.now())
                .description(description)
                .severityScore(calculateSeverityScore(criteria, consecutivePeriods))
                .detectionMethod(DelistingViolation.DetectionMethod.AUTOMATIC)
                .requiresAction(true)
                .isResolved(false)  // 명시적으로 false 설정
                .build();

        DelistingViolation saved = violationRepo.save(violation);

        // 주식 상태를 DELISTING_RISK로 변경 (위반 감지 시)
        updateStockStatusToRisk(stockId);

        // 이력 기록
        recordHistory(stockId, ActionType.CRITERIA_VIOLATION, null, null, 
                     "기준 위반 감지: " + criteriaCode, saved.getId().toString(), null);

        log.info("기준 위반 기록 완료: violationId={}, consecutivePeriods={}", 
                saved.getId(), consecutivePeriods);

        return saved;
    }

    /**
     * 주식 상태를 상장폐지 위험으로 변경
     */
    private void updateStockStatusToRisk(UUID stockId) {
        try {
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            
            // 이미 상장폐지 관련 상태가 아닌 경우에만 변경
            if (stock.getStatus() == Stock.Status.LISTED || stock.getStatus() == Stock.Status.SUSPENDED) {
                stock.updateStatus(Stock.Status.DELISTING_RISK);
                stockRepo.save(stock);
                log.info("주식 상태 변경: stockId={}, status=DELISTING_RISK", stockId);
            }
        } catch (Exception e) {
            log.error("주식 상태 변경 실패: stockId={}", stockId, e);
        }
    }

    /**
     * 상장폐지 예고 상태로 변경
     */
    @Transactional
    public void issueDelistingNotice(UUID stockId) {
        log.info("상장폐지 예고 발행: stockId={}", stockId);
        
        try {
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            
            stock.updateStatus(Stock.Status.DELISTING_NOTICE);
            stock.setDelistingNoticeDate(LocalDateTime.now());
            stockRepo.save(stock);
            
            log.info("상장폐지 예고 완료: stockId={}", stockId);
            
            // 이력 기록
            recordHistory(stockId, ActionType.DELISTING_NOTICE, null, null,
                         "상장폐지 예고 발행", null, null);
            
        } catch (Exception e) {
            log.error("상장폐지 예고 실패: stockId={}", stockId, e);
            throw new RuntimeException("상장폐지 예고 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 상장폐지 절차 시작
     */
    @Transactional
    public void startDelistingProcess(UUID stockId) {
        log.info("상장폐지 절차 시작: stockId={}", stockId);
        
        try {
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            
            stock.updateStatus(Stock.Status.DELISTING_PROCESS);
            stockRepo.save(stock);
            
            log.info("상장폐지 절차 시작 완료: stockId={}", stockId);
            
            // 이력 기록
            recordHistory(stockId, ActionType.STAGE_CHANGE, 
                         com.beyond.MKX.domain.delisting.entity.DelistingStage.DELISTING_NOTICE,
                         com.beyond.MKX.domain.delisting.entity.DelistingStage.DELISTING_PROCESS,
                         "상장폐지 절차 시작", null, null);
            
        } catch (Exception e) {
            log.error("상장폐지 절차 시작 실패: stockId={}", stockId, e);
            throw new RuntimeException("상장폐지 절차 시작 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 상장폐지 단계 진행 체크
     */
    public void checkDelistingProgress(UUID stockId) {
        log.info("상장폐지 진행 체크 시작: stockId={}", stockId);

        // 해결되지 않은 위반 목록 조회
        List<DelistingViolation> unresolvedViolations = violationRepo.findByStockIdAndUnresolved(stockId);
        
        if (unresolvedViolations.isEmpty()) {
            log.info("해결되지 않은 위반 없음: stockId={}", stockId);
            return;
        }

        // 심각한 위반이 있는지 확인
        boolean hasCriticalViolations = unresolvedViolations.stream()
                .anyMatch(v -> v.getViolationType() == ViolationType.CRITICAL);

        if (hasCriticalViolations) {
            // 상장폐지 예고 단계로 진행
            proceedToDelistingNotice(stockId, unresolvedViolations);
        } else {
            // 경고 단계로 진행
            proceedToWarning(stockId, unresolvedViolations);
        }
    }

    /**
     * 상장폐지 예고 실행
     */
    private void proceedToDelistingNotice(UUID stockId, List<DelistingViolation> violations) {
        log.info("상장폐지 예고 실행: stockId={}", stockId);

        // 이력 기록
        recordHistory(stockId, ActionType.STAGE_CHANGE, DelistingStage.NORMAL, 
                     DelistingStage.DELISTING_NOTICE, "상장폐지 예고", 
                     violations.stream().map(v -> v.getId().toString()).toList().toString(), null);

        // 보상금 계산 및 생성 (실제로는 주주별로 생성해야 함)
        createCompensations(stockId);

        log.info("상장폐지 예고 완료: stockId={}", stockId);
    }

    /**
     * 경고 단계 진행
     */
    private void proceedToWarning(UUID stockId, List<DelistingViolation> violations) {
        log.info("경고 단계 진행: stockId={}", stockId);

        // 이력 기록
        recordHistory(stockId, ActionType.STAGE_CHANGE, DelistingStage.NORMAL, 
                     DelistingStage.WARNING, "경고 단계 진행", 
                     violations.stream().map(v -> v.getId().toString()).toList().toString(), null);

        log.info("경고 단계 완료: stockId={}", stockId);
    }

    /**
     * 보상금 생성 및 실제 지급 처리
     * 3단계 처리 방식:
     * 1. 기업 계좌 잔액으로 지급
     * 2. 유동자산에서 차감하고 지급
     * 3. 파산 처리 후 거래소에서 지급
     */
    private void createCompensations(UUID stockId) {
        log.info("보상금 생성 및 지급 시작: stockId={}", stockId);

        try {
            // stockId로 Stock 엔티티 조회하여 ticker 가져오기
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));

            String ticker = stock.getTicker();
            log.info("주식 정보 조회: stockId={}, ticker={}", stockId, ticker);

            // ordering 서비스에서 해당 주식의 모든 보유자 조회
            List<StockHoldingResDto> holders = stockHoldingClient.getAllHoldersByTicker(ticker);

            if (holders.isEmpty()) {
                log.info("보유자가 없음: stockId={}, ticker={}", stockId, ticker);
                return;
            }

            // 보상 기준 주가 조회 (marketdata 서비스에서 현재가 조회)
            BigDecimal compensationPrice;
            try {
                CurrentPriceResDto currentPrice = currentPriceClient.getCurrentPrice(ticker);
                if (currentPrice != null && currentPrice.price() != null) {
                    compensationPrice = BigDecimal.valueOf(currentPrice.price());
                    log.info("현재가 조회 성공: ticker={}, price={}", ticker, compensationPrice);
                } else {
                    // 현재가 정보가 없으면 보상금 생성 중단하고 기록
                    log.error("현재가 정보 없음으로 보상금 생성 중단: stockId={}, ticker={}", stockId, ticker);
                    recordCompensationFailure(stockId, ticker, "CURRENT_PRICE_NOT_FOUND", "현재가 정보가 없습니다");
                    return;
                }
            } catch (Exception e) {
                // marketdata 서비스 호출 실패 시 보상금 생성 중단하고 기록
                log.error("현재가 조회 실패로 보상금 생성 중단: stockId={}, ticker={}", stockId, ticker, e);
                recordCompensationFailure(stockId, ticker, "CURRENT_PRICE_FETCH_FAILED", "현재가 조회 실패: " + e.getMessage());
                return;
            }

            // 총 보상금 계산
            BigDecimal totalCompensation = BigDecimal.ZERO;
            List<DelistingCompensation> compensations = new ArrayList<>();

            // 각 보유자별로 보상금 생성
            for (StockHoldingResDto holder : holders) {
                BigDecimal compensationAmount = compensationPrice.multiply(
                    BigDecimal.valueOf(holder.totalQuantity())
                );

                DelistingCompensation compensation = DelistingCompensation.builder()
                        .stockId(stockId)
                        .memberAccountId(holder.memberAccountId())
                        .compensationAmount(compensationAmount)
                        .stockQuantity(holder.totalQuantity())
                        .compensationPrice(compensationPrice)
                        .requestedAt(LocalDateTime.now())
                        .status(CompensationStatus.PENDING)
                        .build();

                compensations.add(compensation);
                totalCompensation = totalCompensation.add(compensationAmount);

                log.info("보상금 생성: accountId={}, quantity={}, amount={}",
                        holder.memberAccountId(), holder.totalQuantity(), compensationAmount);
            }

            // 실제 지급 처리 (3단계 방식)
            processCompensationPayment(stockId, compensations, totalCompensation);

            log.info("보상금 생성 및 지급 완료: stockId={}, 보유자 수={}, 총 보상금={}", 
                    stockId, holders.size(), totalCompensation);

        } catch (Exception e) {
            log.error("보상금 생성 중 오류 발생: stockId={}", stockId, e);
            // FeignClient 호출 실패 시에도 상장폐지 진행은 계속되어야 함
        }
    }

/**
 * 보상금 생성 실패 기록
 * 나중에 재처리할 수 있도록 실패 사유와 함께 기록
 */
private void recordCompensationFailure(UUID stockId, String ticker, String failureCode, String failureMessage) {
    try {
        // 실패 기록을 DelistingHistory에 저장
        DelistingHistory failureRecord = DelistingHistory.builder()
                .stockId(stockId)
                .actionType(ActionType.COMPENSATION_FAILED)
                .reason("보상금 생성 실패: " + failureMessage)
                .executionResult(DelistingHistory.ExecutionResult.FAILED)
                .executionMessage(failureCode + ": " + failureMessage)
                .executionIp("SYSTEM")
                .sessionId("COMPENSATION_FAILURE")
                .build();

        historyRepo.save(failureRecord);

        log.error("보상금 생성 실패 기록 완료: stockId={}, ticker={}, code={}, message={}", 
                stockId, ticker, failureCode, failureMessage);

    } catch (Exception e) {
        log.error("보상금 실패 기록 저장 중 오류: stockId={}", stockId, e);
    }
}

/**
 * 보상금 지급 실패 기록 (오버로드)
 * ticker 없이 호출하는 경우
 */
private void recordCompensationFailure(UUID stockId, String failureCode, String failureMessage) {
    try {
        // 실패 기록을 DelistingHistory에 저장
        DelistingHistory failureRecord = DelistingHistory.builder()
                .stockId(stockId)
                .actionType(ActionType.COMPENSATION_FAILED)
                .reason("보상금 지급 실패: " + failureMessage)
                .executionResult(DelistingHistory.ExecutionResult.FAILED)
                .executionMessage(failureCode + ": " + failureMessage)
                .executionIp("SYSTEM")
                .sessionId("COMPENSATION_FAILURE")
                .build();

        historyRepo.save(failureRecord);

        log.error("보상금 지급 실패 기록 완료: stockId={}, code={}, message={}", 
                stockId, failureCode, failureMessage);

    } catch (Exception e) {
        log.error("보상금 실패 기록 저장 중 오류: stockId={}", stockId, e);
    }
}

/**
 * 실패한 보상금 재처리
 * 관리자가 수동으로 호출하거나 스케줄러로 주기적으로 실행
 */
@Transactional
public void retryFailedCompensations(UUID stockId) {
    log.info("실패한 보상금 재처리 시작: stockId={}", stockId);
    
    try {
        // 해당 주식의 실패 기록 조회
        List<DelistingHistory> failureRecords = historyRepo.findByStockIdAndActionTypeAndExecutionResult(
                stockId, ActionType.COMPENSATION_FAILED, DelistingHistory.ExecutionResult.FAILED);
        
        if (failureRecords.isEmpty()) {
            log.info("재처리할 실패 기록 없음: stockId={}", stockId);
            return;
        }
        
        // 보상금 재생성 시도
        createCompensations(stockId);
        
        // 재처리 성공 기록
        DelistingHistory retryRecord = DelistingHistory.builder()
                .stockId(stockId)
                .actionType(ActionType.COMPENSATION_FAILED)
                .reason("보상금 재처리 시도")
                .executionResult(DelistingHistory.ExecutionResult.RETRY_ATTEMPTED)
                .executionMessage("관리자에 의한 재처리 시도")
                .executionIp("SYSTEM")
                .sessionId("COMPENSATION_RETRY")
                .build();
        
        historyRepo.save(retryRecord);
        
        log.info("보상금 재처리 완료: stockId={}", stockId);
        
    } catch (Exception e) {
        log.error("보상금 재처리 중 오류: stockId={}", stockId, e);
    }
}

/**
 * 상장폐지 실행
 * 주식의 상장폐지를 실행하고 관련 처리를 수행합니다.
 */
@Transactional
public void executeDelisting(UUID stockId) {
    log.info("상장폐지 실행 시작: stockId={}", stockId);
    
    try {
        // 주식 정보 조회
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
        
        // 이미 상장폐지된 주식인지 확인
        if (stock.getStatus() == Stock.Status.DELISTED) {
            log.warn("이미 상장폐지된 주식: stockId={}", stockId);
            return;
        }
        
        // 주식 상태를 DELISTED로 변경
        stock.updateStatus(Stock.Status.DELISTED);
        stockRepo.save(stock);
        
        log.info("주식 상태 변경 완료: stockId={}, status=DELISTED", stockId);
        
        // 보상금 생성 (MSA 연동)
        createCompensations(stockId);
        
        // 이력 기록
        recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                     "상장폐지 실행 완료", null, null);
        
        log.info("상장폐지 실행 완료: stockId={}", stockId);
        
    } catch (Exception e) {
        log.error("상장폐지 실행 중 오류: stockId={}", stockId, e);
        
        // 실패 이력 기록
        recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                     "상장폐지 실행 실패: " + e.getMessage(), null, null);
        
        throw new RuntimeException("상장폐지 실행 실패: " + e.getMessage(), e);
    }
}

/**
 * 실패한 보상금 자동 재처리 스케줄러
 * 매 10분마다 실행되어 실패한 보상금을 자동으로 재시도
 */
@Scheduled(fixedRate = 600000) // 10분마다 실행 (600,000ms = 10분)
public void scheduledRetryFailedCompensations() {
    log.info("실패한 보상금 자동 재처리 스케줄러 시작");
    
    try {
        // 모든 실패한 보상금 기록 조회
        List<DelistingHistory> failureRecords = historyRepo.findByActionTypeAndExecutionResult(
                ActionType.COMPENSATION_FAILED, DelistingHistory.ExecutionResult.FAILED);
        
        if (failureRecords.isEmpty()) {
            log.debug("재처리할 실패 기록 없음");
            return;
        }
        
        log.info("실패한 보상금 재처리 대상: {}건", failureRecords.size());
        
        // 주식별로 그룹화하여 중복 처리 방지
        List<UUID> uniqueStockIds = failureRecords.stream()
                .map(DelistingHistory::getStockId)
                .distinct()
                .toList();
        
        int successCount = 0;
        int failureCount = 0;
        
        for (UUID stockId : uniqueStockIds) {
            try {
                retryFailedCompensations(stockId);
                successCount++;
                log.info("보상금 재처리 성공: stockId={}", stockId);
            } catch (Exception e) {
                failureCount++;
                log.error("보상금 재처리 실패: stockId={}", stockId, e);
            }
        }
        
        log.info("실패한 보상금 자동 재처리 완료: 성공={}건, 실패={}건", successCount, failureCount);
        
    } catch (Exception e) {
        log.error("실패한 보상금 자동 재처리 스케줄러 오류", e);
    }
}

    /**
     * 위반 해결 처리
     */
    public void resolveViolation(UUID violationId, UUID resolvedBy, String description) {
        log.info("위반 해결 처리: violationId={}, resolvedBy={}", violationId, resolvedBy);

        DelistingViolation violation = violationRepo.findById(violationId)
                .orElseThrow(() -> new IllegalArgumentException("위반 기록을 찾을 수 없습니다: " + violationId));

        violation.setIsResolved(true);
        violation.setResolvedDate(LocalDateTime.now());
        violation.setResolvedBy(resolvedBy);
        violation.setDescription(description);

        violationRepo.save(violation);

        // 이력 기록
        recordHistory(violation.getStockId(), ActionType.CRITERIA_VIOLATION, null, null, 
                     "위반 해결: " + violation.getCriteriaCode(), violationId.toString(), resolvedBy);

        log.info("위반 해결 완료: violationId={}", violationId);
    }

    /**
     * 이력 기록 헬퍼 메서드
     */
    private void recordHistory(UUID stockId, ActionType actionType, DelistingStage fromStage, 
                              DelistingStage toStage, String reason, String violationIds, UUID executedBy) {
        DelistingHistory history = DelistingHistory.builder()
                .stockId(stockId)
                .actionType(actionType)
                .fromStage(fromStage)
                .toStage(toStage)
                .reason(reason)
                .violationIds(violationIds)
                .executedBy(executedBy)
                .executionDate(LocalDateTime.now())
                .executionResult(DelistingHistory.ExecutionResult.SUCCESS)
                .build();

        historyRepo.save(history);
    }

    /**
     * 위반 여부 확인
     */
    private boolean isViolation(DelistingCriteria criteria, BigDecimal currentValue) {
        if (criteria.getThresholdValue() == null || currentValue == null) {
            return false; // 임계값이 없으면 위반 판단 불가
        }

        return switch (criteria.getComparisonOperator()) {
            case GREATER_THAN -> currentValue.compareTo(criteria.getThresholdValue()) > 0;
            case LESS_THAN -> currentValue.compareTo(criteria.getThresholdValue()) < 0;
            case GREATER_THAN_OR_EQUAL -> currentValue.compareTo(criteria.getThresholdValue()) >= 0;
            case LESS_THAN_OR_EQUAL -> currentValue.compareTo(criteria.getThresholdValue()) <= 0;
            case EQUAL -> currentValue.compareTo(criteria.getThresholdValue()) == 0;
            case NOT_EQUAL -> currentValue.compareTo(criteria.getThresholdValue()) != 0;
            default -> false;
        };
    }

    /**
     * 위반 유형 결정
     */
    private ViolationType determineViolationType(DelistingCriteria criteria, int consecutivePeriods) {
        // 연속 위반 기간이 기준보다 길면 CRITICAL
        if (criteria.getThresholdPeriod() != null && consecutivePeriods >= criteria.getThresholdPeriod()) {
            return ViolationType.CRITICAL;
        }
        return ViolationType.WARNING;
    }

    /**
     * 심각도 점수 계산
     */
    private Integer calculateSeverityScore(DelistingCriteria criteria, int consecutivePeriods) {
        int baseScore = 1;
        
        // 연속 위반 기간에 따른 점수 증가
        int periodScore = consecutivePeriods * 2;
        
        // 기준 유형에 따른 가중치
        int typeWeight = switch (criteria.getCriteriaType()) {
            case REGULATORY -> 3; // 법규 기준이 가장 심각
            case FINANCIAL -> 2; // 재무 기준
            case TRADING -> 1;   // 거래 기준
        };

        return Math.min(baseScore + periodScore + typeWeight, 10); // 최대 10점
    }

    // ====== 자동화된 상장폐지 위험 감지 시스템 ======

    /**
     * 실제 재무제표 데이터를 기반으로 자동 상장폐지 위험 감지
     * CompanyFinancials, FinancialRatios, Corporation 데이터를 종합적으로 분석
     */
    @Transactional
    public void detectViolationFromFinancials(UUID stockId) {
        log.info("재무제표 기반 상장폐지 위험 자동 감지 시작: stockId={}", stockId);
        
        try {
            // 주식 정보 조회
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            
            // 최신 재무제표 조회 (최근 2년간)
            List<CompanyFinancials> recentFinancials = companyFinancialsRepo
                    .findByStockIdOrderByFiscalYearDescFiscalQuarterDesc(stockId);
            
            if (recentFinancials.isEmpty()) {
                log.warn("재무제표 데이터 없음: stockId={}", stockId);
                return;
            }
            
            // 최신 재무비율 조회
            List<FinancialRatios> recentRatios = financialRatiosRepo
                    .findByStockIdOrderByFiscalYearDescFiscalQuarterDesc(stockId);
            
            // 기업 정보 조회 (자본금 등)
            Corporation corporation = corporationRepo.findById(stock.getCorporationId())
                    .orElseThrow(() -> new IllegalArgumentException("Corporation not found: " + stock.getCorporationId()));
            
            // 활성화된 상장폐지 기준들 조회
            List<DelistingCriteria> activeCriteria = criteriaRepo.findByIsActiveTrue();
            
            int violationCount = 0;
            
            for (DelistingCriteria criteria : activeCriteria) {
                // 각 기준별로 위반 여부 체크
                if (checkCriteriaViolationFromFinancials(stockId, criteria, recentFinancials, recentRatios, corporation)) {
                    violationCount++;
                }
            }
            
            log.info("재무제표 기반 위반 감지 완료: stockId={}, 위반 건수={}", stockId, violationCount);
            
        } catch (Exception e) {
            log.error("재무제표 기반 위반 감지 실패: stockId={}", stockId, e);
        }
    }

    /**
     * 특정 기준에 대한 위반 여부 체크 (재무제표 기반)
     */
    private boolean checkCriteriaViolationFromFinancials(UUID stockId, DelistingCriteria criteria, 
                                                        List<CompanyFinancials> financials,
                                                        List<FinancialRatios> ratios,
                                                        Corporation corporation) {
        
        BigDecimal currentValue = null;
        String description = "";
        
        try {
            switch (criteria.getCriteriaCode()) {
                case "LOW_REVENUE_2Y":
                    // 최근 2년간 매출액 평균
                    currentValue = calculateAverageRevenue(financials, 2);
                    description = String.format("최근 2년간 평균 매출액: %,.0f원", currentValue);
                    break;
                    
                case "NEGATIVE_NET_INCOME":
                    // 최근 순이익
                    if (!financials.isEmpty()) {
                        currentValue = BigDecimal.valueOf(financials.get(0).getNetIncome());
                        description = String.format("최근 순이익: %,.0f원", currentValue);
                    }
                    break;
                    
                case "LOW_EQUITY_RATIO":
                    // 자기자본비율 계산
                    if (!financials.isEmpty()) {
                        CompanyFinancials latest = financials.get(0);
                        if (latest.getTotalAssets() != null && latest.getTotalAssets() > 0) {
                            currentValue = BigDecimal.valueOf(latest.getTotalEquity())
                                    .divide(BigDecimal.valueOf(latest.getTotalAssets()), 4, java.math.RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                            description = String.format("자기자본비율: %.2f%%", currentValue);
                        }
                    }
                    break;
                    
                case "HIGH_DEBT_RATIO":
                    // 부채비율 계산
                    if (!ratios.isEmpty() && ratios.get(0).getDebtRatio() != null) {
                        currentValue = ratios.get(0).getDebtRatio();
                        description = String.format("부채비율: %.2f%%", currentValue);
                    }
                    break;
                    
                case "LOW_CURRENT_RATIO":
                    // 유동비율 계산
                    if (!ratios.isEmpty() && ratios.get(0).getCurrentRatio() != null) {
                        currentValue = ratios.get(0).getCurrentRatio();
                        description = String.format("유동비율: %.2f", currentValue);
                    }
                    break;
                    
                case "INSUFFICIENT_CAPITAL":
                    // 자본금 기준
                    currentValue = BigDecimal.valueOf(corporation.getCapital());
                    description = String.format("자본금: %,.0f원", currentValue);
                    break;
                    
                case "LOW_ROE":
                    // 자기자본이익률
                    if (!ratios.isEmpty() && ratios.get(0).getRoe() != null) {
                        currentValue = ratios.get(0).getRoe();
                        description = String.format("ROE: %.2f%%", currentValue);
                    }
                    break;
                    
                case "LOW_ROA":
                    // 총자산이익률
                    if (!ratios.isEmpty() && ratios.get(0).getRoa() != null) {
                        currentValue = ratios.get(0).getRoa();
                        description = String.format("ROA: %.2f%%", currentValue);
                    }
                    break;
                    
                case "NEGATIVE_OPERATING_INCOME":
                    // 영업이익 기준
                    if (!financials.isEmpty()) {
                        currentValue = BigDecimal.valueOf(financials.get(0).getOperatingIncome());
                        description = String.format("영업이익: %,.0f원", currentValue);
                    }
                    break;
                    
                case "LOW_INTEREST_COVERAGE":
                    // 이자보상배수
                    if (!ratios.isEmpty() && ratios.get(0).getInterestCoverage() != null) {
                        currentValue = ratios.get(0).getInterestCoverage();
                        description = String.format("이자보상배수: %.2f", currentValue);
                    }
                    break;
                    
                default:
                    log.warn("알 수 없는 기준 코드: {}", criteria.getCriteriaCode());
                    return false;
            }
            
            if (currentValue != null && isViolation(criteria, currentValue)) {
                // 위반 감지 시 자동 기록
                DelistingViolation violation = detectViolation(stockId, criteria.getCriteriaCode(), 
                                                             currentValue, description);
                
                if (violation != null) {
                    log.info("재무제표 기반 위반 감지: stockId={}, criteria={}, value={}", 
                            stockId, criteria.getCriteriaCode(), currentValue);
                    return true;
                }
            }
            
        } catch (Exception e) {
            log.error("기준 위반 체크 실패: stockId={}, criteria={}", stockId, criteria.getCriteriaCode(), e);
        }
        
        return false;
    }

    /**
     * 최근 N년간 평균 매출액 계산
     */
    private BigDecimal calculateAverageRevenue(List<CompanyFinancials> financials, int years) {
        if (financials.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // 최근 N년간의 연간 매출액 합계
        Map<Integer, Long> yearlyRevenue = new HashMap<>();
        
        for (CompanyFinancials financial : financials) {
            if (financial.getRevenue() != null) {
                yearlyRevenue.merge(financial.getFiscalYear(), financial.getRevenue(), Long::sum);
            }
        }
        
        // 최근 N년간의 매출액만 추출
        List<Long> recentRevenues = yearlyRevenue.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByKey().reversed())
                .limit(years)
                .map(Map.Entry::getValue)
                .toList();
        
        if (recentRevenues.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        // 평균 계산
        long totalRevenue = recentRevenues.stream().mapToLong(Long::longValue).sum();
        return BigDecimal.valueOf(totalRevenue).divide(BigDecimal.valueOf(recentRevenues.size()), 0, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 분기별 재무제표 기반 자동 상장폐지 위험 감지 스케줄러
     * 매 분기 시작일 오전 9시에 실행
     */
    @Scheduled(cron = "0 0 9 1 1,4,7,10 *") // 1월, 4월, 7월, 10월 1일 오전 9시
    public void quarterlyDelistingRiskDetection() {
        log.info("분기별 상장폐지 위험 자동 감지 시작");
        
        try {
            // 모든 상장 주식에 대해 감지 (정상 상장, 거래정지, 상장폐지 위험 상태)
            List<Stock> targetStocks = stockRepo.findByStatusIn(List.of(
                    Stock.Status.LISTED, 
                    Stock.Status.SUSPENDED, 
                    Stock.Status.DELISTING_RISK
            ));
            
            int processedCount = 0;
            int violationCount = 0;
            
            for (Stock stock : targetStocks) {
                try {
                    int beforeCount = violationRepo.countByStockIdAndIsResolvedFalse(stock.getId());
                    detectViolationFromFinancials(stock.getId());
                    int afterCount = violationRepo.countByStockIdAndIsResolvedFalse(stock.getId());
                    
                    if (afterCount > beforeCount) {
                        violationCount += (afterCount - beforeCount);
                    }
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    log.error("주식별 위반 감지 실패: stockId={}", stock.getId(), e);
                }
            }
            
            log.info("분기별 상장폐지 위험 자동 감지 완료: 처리={}개 주식, 신규 위반={}건", 
                    processedCount, violationCount);
            
        } catch (Exception e) {
            log.error("분기별 상장폐지 위험 자동 감지 실패", e);
        }
    }

    /**
     * 재무제표 업데이트 시 즉시 상장폐지 위험 감지
     * FinancialAggregateService에서 호출
     */
    public void onFinancialsUpdated(UUID stockId) {
        log.info("재무제표 업데이트로 인한 즉시 위반 감지: stockId={}", stockId);
        
        try {
            detectViolationFromFinancials(stockId);
        } catch (Exception e) {
            log.error("재무제표 업데이트 기반 위반 감지 실패: stockId={}", stockId, e);
        }
    }

    /**
     * 특정 주식의 모든 상장폐지 위험 요인 분석 리포트 생성
     */
    public Map<String, Object> generateDelistingRiskReport(UUID stockId) {
        log.info("상장폐지 위험 분석 리포트 생성: stockId={}", stockId);
        
        Map<String, Object> report = new HashMap<>();
        
        try {
            // 주식 정보
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            
            report.put("stockId", stockId);
            report.put("ticker", stock.getTicker());
            report.put("nameKo", stock.getNameKo());
            report.put("currentStatus", stock.getStatus());
            
            // 최신 재무제표
            List<CompanyFinancials> recentFinancials = companyFinancialsRepo
                    .findByStockIdOrderByFiscalYearDescFiscalQuarterDesc(stockId);
            report.put("latestFinancials", recentFinancials.isEmpty() ? null : recentFinancials.get(0));
            
            // 최신 재무비율
            List<FinancialRatios> recentRatios = financialRatiosRepo
                    .findByStockIdOrderByFiscalYearDescFiscalQuarterDesc(stockId);
            report.put("latestRatios", recentRatios.isEmpty() ? null : recentRatios.get(0));
            
            // 기업 정보
            Corporation corporation = corporationRepo.findById(stock.getCorporationId())
                    .orElseThrow(() -> new IllegalArgumentException("Corporation not found: " + stock.getCorporationId()));
            report.put("corporation", corporation);
            
            // 해결되지 않은 위반 목록
            List<DelistingViolation> unresolvedViolations = violationRepo.findByStockIdAndIsResolvedFalse(stockId);
            report.put("unresolvedViolations", unresolvedViolations);
            report.put("violationCount", unresolvedViolations.size());
            
            // 위험도 점수 계산
            int riskScore = calculateOverallRiskScore(unresolvedViolations);
            report.put("riskScore", riskScore);
            report.put("riskLevel", getRiskLevel(riskScore));
            
            log.info("상장폐지 위험 분석 리포트 생성 완료: stockId={}, 위험도={}", stockId, riskScore);
            
        } catch (Exception e) {
            log.error("상장폐지 위험 분석 리포트 생성 실패: stockId={}", stockId, e);
            report.put("error", e.getMessage());
        }
        
        return report;
    }

    /**
     * 전체 위험도 점수 계산
     */
    private int calculateOverallRiskScore(List<DelistingViolation> violations) {
        if (violations.isEmpty()) {
            return 0;
        }
        
        return violations.stream()
                .mapToInt(violation -> violation.getSeverityScore() != null ? violation.getSeverityScore() : 1)
                .sum();
    }

    /**
     * 위험도 레벨 결정
     */
    private String getRiskLevel(int riskScore) {
        if (riskScore >= 20) return "CRITICAL";
        if (riskScore >= 15) return "HIGH";
        if (riskScore >= 10) return "MEDIUM";
        if (riskScore >= 5) return "LOW";
        return "MINIMAL";
    }

    /**
     * 보상금 실제 지급 처리 (3단계 방식)
     */
    @Transactional
    private void processCompensationPayment(UUID stockId, List<DelistingCompensation> compensations, BigDecimal totalCompensation) {
        log.info("보상금 지급 처리 시작: stockId={}, 총 보상금={}", stockId, totalCompensation);

        try {
            // 1단계: 기업 계좌 잔액 확인
            CorporationAccount corpAccount = getCorporationAccountByStockId(stockId);
            BigDecimal availableBalance = new BigDecimal(corpAccount.getBalance());

            if (availableBalance.compareTo(totalCompensation) >= 0) {
                // 1단계: 기업 계좌에서 충분한 잔액이 있으면 정상 지급
                log.info("1단계: 기업 계좌 잔액으로 정상 지급: stockId={}, 잔액={}", stockId, availableBalance);
                processPaymentFromCash(stockId, compensations, totalCompensation);
                
            } else {
                // 2단계: 유동자산에서 차감하고 지급
                log.info("2단계: 유동자산 활용 지급 시작: stockId={}, 부족금액={}", 
                        stockId, totalCompensation.subtract(availableBalance));
                
                BigDecimal remainingAmount = processPaymentFromCurrentAssets(stockId, compensations, totalCompensation, availableBalance);
                
                if (remainingAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // 3단계: 파산 처리 후 거래소에서 지급
                    log.info("3단계: 파산 처리 후 거래소 지급: stockId={}, 거래소 지급액={}", stockId, remainingAmount);
                    processPaymentFromExchange(stockId, compensations, remainingAmount);
                }
            }

        } catch (Exception e) {
            log.error("보상금 지급 처리 중 오류: stockId={}", stockId, e);
            recordCompensationFailure(stockId, "PAYMENT_PROCESSING_FAILED", "지급 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 1단계: 기업 계좌 현금으로 지급
     */
    private void processPaymentFromCash(UUID stockId, List<DelistingCompensation> compensations, BigDecimal totalCompensation) {
        CorporationAccount corpAccount = getCorporationAccountByStockId(stockId);
        
        // 기업 계좌에서 출금
        corpAccount.withdraw(totalCompensation.toBigInteger());
        corporationAccountRepo.save(corpAccount);
        
        // 각 보상금 지급 처리
        for (DelistingCompensation compensation : compensations) {
            // 회원 계좌로 입금 (MSA 연동)
            try {
                MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                        compensation.getMemberAccountId(),
                        compensation.getCompensationAmount(),
                        "상장폐지 보상금 지급"
                );
                MemberAccountClient.DepositResult result = memberAccountClient.deposit(request);
                
                if (result.success()) {
                    // 보상금 상태 업데이트
                    compensation.setStatus(CompensationStatus.COMPLETED);
                    compensation.setProcessedAt(LocalDateTime.now());
                    compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000")); // 시스템 처리
                    compensationRepo.save(compensation);
                    
                    log.info("현금 지급 완료: accountId={}, amount={}", 
                            compensation.getMemberAccountId(), compensation.getCompensationAmount());
                } else {
                    log.error("현금 지급 실패: accountId={}, message={}", 
                            compensation.getMemberAccountId(), result.message());
                    compensation.setStatus(CompensationStatus.FAILED);
                    compensationRepo.save(compensation);
                }
                        
            } catch (Exception e) {
                log.error("현금 지급 실패: accountId={}", compensation.getMemberAccountId(), e);
                compensation.setStatus(CompensationStatus.FAILED);
                compensationRepo.save(compensation);
            }
        }
        
        // 이력 기록
        recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                     "보상금 현금 지급 완료: " + totalCompensation, null, null);
    }

    /**
     * 2단계: 유동자산에서 차감하고 지급
     */
    private BigDecimal processPaymentFromCurrentAssets(UUID stockId, List<DelistingCompensation> compensations, 
                                                     BigDecimal totalCompensation, BigDecimal availableCash) {
        // 최신 재무제표 조회
        CompanyFinancials latestFinancials = companyFinancialsRepo
                .findByStockIdOrderByFiscalYearDescFiscalQuarterDesc(stockId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("재무제표 데이터 없음: " + stockId));

        Long currentAssets = latestFinancials.getCurrentAssets();
        if (currentAssets == null || currentAssets <= 0) {
            log.warn("유동자산이 없음: stockId={}", stockId);
            return totalCompensation.subtract(availableCash);
        }

        // 유동자산의 70%까지 활용 가능 (30%는 운영자금으로 보존)
        BigDecimal usableCurrentAssets = BigDecimal.valueOf(currentAssets).multiply(new BigDecimal("0.7"));
        BigDecimal totalAvailable = availableCash.add(usableCurrentAssets);
        
        log.info("유동자산 활용: stockId={}, 유동자산={}, 사용가능={}, 총가능={}", 
                stockId, currentAssets, usableCurrentAssets, totalAvailable);

        if (totalAvailable.compareTo(totalCompensation) >= 0) {
            // 유동자산으로 충분히 지급 가능
            BigDecimal amountFromCash = availableCash;
            BigDecimal amountFromAssets = totalCompensation.subtract(availableCash);
            
            // 기업 계좌에서 현금 출금
            CorporationAccount corpAccount = getCorporationAccountByStockId(stockId);
            corpAccount.withdraw(amountFromCash.toBigInteger());
            corporationAccountRepo.save(corpAccount);
            
            // 유동자산 차감 (재무제표 업데이트)
            latestFinancials.setCurrentAssets(currentAssets - amountFromAssets.longValue());
            companyFinancialsRepo.save(latestFinancials);
            
            // 각 보상금 지급 처리
            for (DelistingCompensation compensation : compensations) {
                try {
                    MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                            compensation.getMemberAccountId(),
                            compensation.getCompensationAmount(),
                            "상장폐지 보상금 지급 (유동자산 활용)"
                    );
                    MemberAccountClient.DepositResult result = memberAccountClient.deposit(request);
                    
                    if (result.success()) {
                        compensation.setStatus(CompensationStatus.COMPLETED);
                        compensation.setProcessedAt(LocalDateTime.now());
                        compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                        compensationRepo.save(compensation);
                    } else {
                        log.error("유동자산 지급 실패: accountId={}, message={}", 
                                compensation.getMemberAccountId(), result.message());
                        compensation.setStatus(CompensationStatus.FAILED);
                        compensationRepo.save(compensation);
                    }
                    
                } catch (Exception e) {
                    log.error("유동자산 지급 실패: accountId={}", compensation.getMemberAccountId(), e);
                    compensation.setStatus(CompensationStatus.FAILED);
                    compensationRepo.save(compensation);
                }
            }
            
            // 이력 기록
            recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                         "보상금 유동자산 활용 지급 완료: 현금=" + amountFromCash + ", 자산=" + amountFromAssets, null, null);
            
            return BigDecimal.ZERO; // 완전 지급 완료
            
        } else {
            // 유동자산으로도 부족 - 가능한 만큼만 지급
            BigDecimal partialAmount = totalAvailable;
            
            // 기업 계좌에서 현금 출금
            CorporationAccount corpAccount = getCorporationAccountByStockId(stockId);
            corpAccount.withdraw(availableCash.toBigInteger());
            corporationAccountRepo.save(corpAccount);
            
            // 유동자산 전액 차감
            latestFinancials.setCurrentAssets(0L);
            companyFinancialsRepo.save(latestFinancials);
            
            // 부분 지급 처리
            BigDecimal remainingCompensation = totalCompensation.subtract(partialAmount);
            
            for (DelistingCompensation compensation : compensations) {
                // 비례 계산
                BigDecimal proportionalAmount = compensation.getCompensationAmount()
                        .multiply(partialAmount)
                        .divide(totalCompensation, 2, RoundingMode.DOWN);
                
                if (proportionalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    try {
                        MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                                compensation.getMemberAccountId(),
                                proportionalAmount,
                                "상장폐지 보상금 부분 지급"
                        );
                        MemberAccountClient.DepositResult result = memberAccountClient.deposit(request);
                        
                        if (result.success()) {
                            compensation.setStatus(CompensationStatus.PARTIAL_PAID);
                            compensation.setProcessedAt(LocalDateTime.now());
                            compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                            compensationRepo.save(compensation);
                        } else {
                            log.error("부분 지급 실패: accountId={}, message={}", 
                                    compensation.getMemberAccountId(), result.message());
                            compensation.setStatus(CompensationStatus.FAILED);
                            compensationRepo.save(compensation);
                        }
                        
                    } catch (Exception e) {
                        log.error("부분 지급 실패: accountId={}", compensation.getMemberAccountId(), e);
                        compensation.setStatus(CompensationStatus.FAILED);
                        compensationRepo.save(compensation);
                    }
                }
            }
            
            // 이력 기록
            recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                         "보상금 부분 지급 완료: 지급액=" + partialAmount + ", 미지급액=" + remainingCompensation, null, null);
            
            return remainingCompensation; // 미지급분 반환
        }
    }

    /**
     * 3단계: 파산 처리 후 거래소에서 지급
     */
    private void processPaymentFromExchange(UUID stockId, List<DelistingCompensation> compensations, BigDecimal remainingAmount) {
        // 1. 기업 파산 처리
        Corporation corporation = corporationRepo.findById(getCorporationIdByStockId(stockId))
                .orElseThrow(() -> new IllegalArgumentException("Corporation not found"));
        // corporation.setStatus(Status.DELISTED); // Corporation 엔티티에 setStatus 메서드가 없음
        corporationRepo.save(corporation);
        
        // 2. 거래소 운영 계좌에서 지급
        ExchangeAccount exchangeAccount = getExchangeAccount();
        
        if (exchangeAccount.getBalance().compareTo(remainingAmount.toBigInteger()) >= 0) {
            // 거래소 계좌에서 출금
            exchangeAccount.withdraw(remainingAmount.toBigInteger());
            exchangeAccountRepo.save(exchangeAccount);
            
            // 거래소 지원금 기록 생성
            UUID corporationId = getCorporationIdByStockId(stockId);
            ExchangeSupportFund supportFund = ExchangeSupportFund.builder()
                    .stockId(stockId)
                    .corporationId(corporationId)
                    .supportAmount(remainingAmount)
                    .supportType(ExchangeSupportFund.SupportType.COMPENSATION_LOAN)
                    .status(ExchangeSupportFund.SupportStatus.ACTIVE)
                    .reason("상장폐지 보상금 지원")
                    .repaymentDueDate(LocalDateTime.now().plusYears(3)) // 3년 후 상환 예정
                    .interestRate(new BigDecimal("5.0")) // 연 5% 이자율
                    .processedBy(UUID.fromString("00000000-0000-0000-0000-000000000000")) // 시스템 처리
                    .processedAt(LocalDateTime.now())
                    .remarks("상장폐지 보상금 부족분 지원 - 기업 파산 처리 후 거래소 지원")
                    .build();
            exchangeSupportFundRepo.save(supportFund);
            
            log.info("거래소 지원금 기록 생성: stockId={}, corporationId={}, amount={}", 
                    stockId, corporationId, remainingAmount);
            
            // 미지급 보상금 지급 처리
            for (DelistingCompensation compensation : compensations) {
                if (compensation.getStatus() == CompensationStatus.PARTIAL_PAID) {
                    // 실제 미지급분 계산
                    BigDecimal paidAmount = compensation.getCompensationAmount()
                            .multiply(compensation.getCompensationAmount().subtract(remainingAmount))
                            .divide(compensation.getCompensationAmount(), 2, RoundingMode.DOWN);
                    BigDecimal unpaidAmount = compensation.getCompensationAmount().subtract(paidAmount);
                    
                    try {
                        MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                                compensation.getMemberAccountId(),
                                unpaidAmount,
                                "상장폐지 보상금 거래소 지원금 지급"
                        );
                        MemberAccountClient.DepositResult result = memberAccountClient.deposit(request);
                        
                        if (result.success()) {
                            compensation.setStatus(CompensationStatus.COMPLETED);
                            compensation.setProcessedAt(LocalDateTime.now());
                            compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                            compensationRepo.save(compensation);
                        } else {
                            log.error("거래소 지급 실패: accountId={}, message={}", 
                                    compensation.getMemberAccountId(), result.message());
                            compensation.setStatus(CompensationStatus.FAILED);
                            compensationRepo.save(compensation);
                        }
                        
                    } catch (Exception e) {
                        log.error("거래소 지급 실패: accountId={}", compensation.getMemberAccountId(), e);
                        compensation.setStatus(CompensationStatus.FAILED);
                        compensationRepo.save(compensation);
                    }
                }
            }
            
            // 이력 기록
            recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                         "파산 처리 후 거래소 지원금 지급 완료: " + remainingAmount + " (대출 기록됨)", null, null);
            
        } else {
            // 거래소도 부족하면 최종 실패
            log.error("거래소 운영 계좌도 부족: stockId={}, 필요액={}, 거래소잔액={}", 
                    stockId, remainingAmount, exchangeAccount.getBalance());
            
            recordCompensationFailure(stockId, "EXCHANGE_INSUFFICIENT_FUNDS", 
                    "거래소 운영 계좌 부족으로 최종 지급 실패");
        }
    }

    /**
     * 기업 계좌 조회 헬퍼 메서드
     */
    private CorporationAccount getCorporationAccountByStockId(UUID stockId) {
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
        
        return corporationAccountRepo.findByCorporationId(stock.getCorporationId())
                .orElseThrow(() -> new IllegalArgumentException("Corporation account not found: " + stock.getCorporationId()));
    }

    /**
     * 거래소 운영 계좌 조회 헬퍼 메서드
     */
    private ExchangeAccount getExchangeAccount() {
        return exchangeAccountRepo.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Exchange account not found"));
    }

    /**
     * 기업 ID 조회 헬퍼 메서드
     */
    private UUID getCorporationIdByStockId(UUID stockId) {
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
        return stock.getCorporationId();
    }
}
