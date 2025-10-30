package com.beyond.MKX.domain.delisting.service;

import com.beyond.MKX.common.apiResponse.CommonDTO;
import com.beyond.MKX.domain.delisting.client.CurrentPriceClient;
import com.beyond.MKX.domain.delisting.client.StockHoldingClient;
import com.beyond.MKX.domain.delisting.client.MemberAccountClient;
import com.beyond.MKX.domain.delisting.dto.CurrentPriceResDto;
import com.beyond.MKX.domain.delisting.dto.StockHoldingResDto;
import com.beyond.MKX.domain.delisting.dto.DelistingProgressResDto;
import com.beyond.MKX.domain.delisting.dto.ViolationSummaryDto;
import com.beyond.MKX.domain.delisting.dto.CompensationStatusDto;
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
import com.beyond.MKX.domain.account.accountlist.repository.AccountListRepository;
import com.beyond.MKX.domain.account.accountlist.entity.AccountList;
import com.beyond.MKX.domain.account.accountlist.entity.AccountType;
import com.beyond.MKX.common.openai.OpenAiService;
import com.beyond.MKX.common.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final AccountListRepository accountListRepo;
    private final OpenAiService openAiService;
    private final GptAnalysisService gptAnalysisService;
    
    // Kafka 이벤트 발행을 위한 KafkaTemplate
    private final KafkaTemplate<String, TransactionEvent> transactionKafkaTemplate;
    private static final String TRANSACTION_TOPIC = "transaction-events";

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

        // 최근 24시간 내 동일한 기준으로 해결되지 않은 위반이 있는지 확인
        LocalDateTime recentTime = LocalDateTime.now().minusHours(24);
        List<DelistingViolation> recentViolations = violationRepo.findByStockIdAndCriteriaCodeAndViolationDateAfterAndIsResolvedFalse(
                stockId, criteriaCode, recentTime);
        
        if (!recentViolations.isEmpty()) {
            log.info("최근 24시간 내 동일한 기준 위반이 이미 존재함: stockId={}, criteriaCode={}, count={}", 
                    stockId, criteriaCode, recentViolations.size());
            // 기존 위반이 있어도 상태 확인 및 업데이트 필요
            updateStockStatusToRisk(stockId);
            return recentViolations.get(0); // 기존 위반 반환
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
     * GPT 분석 결과를 포함한 기준 위반 감지 및 기록
     */
    public DelistingViolation detectViolationWithGptAnalysis(UUID stockId, String criteriaCode, 
                                                           BigDecimal currentValue, String description,
                                                           BigDecimal gptRiskScore, String gptAnalysisDescription,
                                                           String gptAnalysisReasoning, boolean gptAnalysisUsed) {
        log.info("GPT 분석 결과 포함 기준 위반 감지 시작: stockId={}, criteriaCode={}, currentValue={}, gptUsed={}", 
                stockId, criteriaCode, currentValue, gptAnalysisUsed);

        // 기준 조회
        DelistingCriteria criteria = criteriaRepo.findByCriteriaCodeAndActive(criteriaCode)
                .orElseThrow(() -> new IllegalArgumentException("활성화된 기준을 찾을 수 없습니다: " + criteriaCode));

        // 위반 여부 확인
        if (!isViolation(criteria, currentValue)) {
            log.info("기준 위반 아님: stockId={}, criteriaCode={}", stockId, criteriaCode);
            return null;
        }

        // 최근 24시간 내 동일한 기준으로 해결되지 않은 위반이 있는지 확인
        LocalDateTime recentTime = LocalDateTime.now().minusHours(24);
        List<DelistingViolation> recentViolations = violationRepo.findByStockIdAndCriteriaCodeAndViolationDateAfterAndIsResolvedFalse(
                stockId, criteriaCode, recentTime);
        
        if (!recentViolations.isEmpty()) {
            log.info("최근 24시간 내 동일한 기준 위반이 이미 존재함: stockId={}, criteriaCode={}, count={}", 
                    stockId, criteriaCode, recentViolations.size());
            // 기존 위반이 있어도 상태 확인 및 업데이트 필요
            updateStockStatusToRisk(stockId);
            return recentViolations.get(0); // 기존 위반 반환
        }

        // 기존 연속 위반 확인
        List<DelistingViolation> existingViolations = violationRepo.findConsecutiveViolations(stockId, criteria.getId());
        int consecutivePeriods = existingViolations.size() + 1;

        // 위반 유형 결정
        ViolationType violationType = determineViolationType(criteria, consecutivePeriods);

        // 위반 기록 생성 (GPT 분석 결과 포함)
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
                .gptRiskScore(gptRiskScore)
                .gptAnalysisDescription(gptAnalysisDescription)
                .gptAnalysisReasoning(gptAnalysisReasoning)
                .gptAnalysisUsed(gptAnalysisUsed)
                .build();

        DelistingViolation saved = violationRepo.save(violation);

        // 주식 상태를 DELISTING_RISK로 변경 (위반 감지 시)
        updateStockStatusToRisk(stockId);

        // 이력 기록
        recordHistory(stockId, ActionType.CRITERIA_VIOLATION, null, null, 
                     "기준 위반 감지 (GPT 분석 포함): " + criteriaCode, saved.getId().toString(), null);

        log.info("GPT 분석 결과 포함 기준 위반 기록 완료: violationId={}, consecutivePeriods={}, gptUsed={}", 
                saved.getId(), consecutivePeriods, gptAnalysisUsed);

        return saved;
    }

    /**
     * 주식 상태를 상장폐지 위험으로 변경
     * 위반 감지 시 자동으로 DELISTING_RISK 상태로 변경
     * 이후 3분 후 자동으로 DELISTING_PROCESS 상태로 전환
     */
    @Transactional
    private void updateStockStatusToRisk(UUID stockId) {
        try {
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            
            // DELISTING_PROCESS 이후 단계는 변경하지 않음
            if (stock.getStatus() == Stock.Status.DELISTING_PROCESS || 
                stock.getStatus() == Stock.Status.DELISTED) {
                log.info("이미 상장폐지 진행 중이거나 완료된 주식: stockId={}, status={}", 
                        stockId, stock.getStatus());
                return;
            }
            
            // 상태가 변경되지 않았거나, stage가 설정되지 않은 경우 업데이트
            boolean needsUpdate = false;
            
            if (stock.getStatus() == Stock.Status.LISTED || stock.getStatus() == Stock.Status.SUSPENDED) {
                // 정상 상장 → 상장폐지 위험
                stock.updateStatus(Stock.Status.DELISTING_RISK);
                stock.setDelistingStage(DelistingStage.WARNING);
                needsUpdate = true;
                log.info("주식 상태 변경: stockId={}, status=LISTED/SUSPENDED→DELISTING_RISK, stage=NORMAL→WARNING", stockId);
            } else if (stock.getStatus() == Stock.Status.DELISTING_RISK && stock.getDelistingStage() == null) {
                // 이미 DELISTING_RISK 상태인데 stage가 설정되지 않은 경우
                stock.setDelistingStage(DelistingStage.WARNING);
                needsUpdate = true;
                log.info("주식 stage 설정: stockId={}, status=DELISTING_RISK, stage=WARNING", stockId);
            }
            
            if (needsUpdate) {
                stockRepo.save(stock);
                log.info("주식 상태 업데이트 완료: stockId={}, status={}, stage={}", 
                        stockId, stock.getStatus(), stock.getDelistingStage());
                
                // 위반이 해결되지 않은 경우, 3분 후 자동으로 DELISTING_PROCESS로 전환
                scheduleAutoDelistingProcess(stockId);
            }
        } catch (Exception e) {
            log.error("주식 상태 변경 실패: stockId={}", stockId, e);
        }
    }
    
    /**
     * 3분 후 자동으로 DELISTING_PROCESS로 전환하는 스케줄러
     * 공시 미제출 시 자동 진행
     */
    private void scheduleAutoDelistingProcess(UUID stockId) {
        // 여기서는 단순히 기록만 하고, 별도의 스케줄러에서 처리
        log.info("상장폐지 자동 진행 예약: stockId={}, 3분 후 DELISTING_PROCESS로 전환 예정", stockId);
        // TODO: 별도 스케줄러에서 처리하거나, 이벤트 발행
    }
    
    /**
     * DelistingStage 순차 검증 및 자동 설정
     * @param currentStage 현재 단계 (null 허용)
     * @param newStage 새 단계
     * @param stock Stock 엔티티
     * @throws IllegalStateException 허용되지 않은 전환인 경우
     * @return 실제 currentStage (자동 설정 후)
     */
    private DelistingStage validateAndAutoSetDelistingStage(DelistingStage currentStage, DelistingStage newStage, Stock stock) {
        // currentStage가 null이면 stock 상태에 따라 자동 설정
        if (currentStage == null) {
            currentStage = inferDelistingStageFromStatus(stock);
            if (currentStage != null) {
                stock.setDelistingStage(currentStage);
                log.info("delisting_stage 자동 설정: stockId={}, status={}, stage={}", 
                        stock.getId(), stock.getStatus(), currentStage);
            } else {
                currentStage = DelistingStage.NORMAL;
            }
        }
        
        // 동일한 stage로 전환하는 것은 허용하지 않음 (이미 완료된 상태)
        if (currentStage == newStage) {
            log.info("이미 목표 stage에 도달: stockId={}, stage={}", stock.getId(), currentStage);
            return currentStage;
        }
        
        Map<DelistingStage, List<DelistingStage>> allowedTransitions = Map.of(
            DelistingStage.NORMAL, List.of(DelistingStage.WARNING, DelistingStage.CAUTION, DelistingStage.DELISTING_NOTICE),
            DelistingStage.WARNING, List.of(DelistingStage.CAUTION, DelistingStage.DELISTING_NOTICE),
            DelistingStage.CAUTION, List.of(DelistingStage.DELISTING_NOTICE),
            DelistingStage.DELISTING_NOTICE, List.of(DelistingStage.DELISTING_PROCESS),
            DelistingStage.DELISTING_PROCESS, List.of(DelistingStage.DELISTED)
        );
        
        List<DelistingStage> allowed = allowedTransitions.get(currentStage);
        if (allowed == null || !allowed.contains(newStage)) {
            throw new IllegalStateException(
                String.format("상장폐지 단계 전환이 불가능합니다. 현재: %s, 요청: %s. 허용된 전환: %s", 
                    currentStage, newStage, allowed)
            );
        }
        
        log.info("상장폐지 단계 전환 검증 통과: {} → {}", currentStage, newStage);
        return currentStage;
    }
    
    /**
     * Stock의 status로부터 DelistingStage 추론
     */
    private DelistingStage inferDelistingStageFromStatus(Stock stock) {
        return switch (stock.getStatus()) {
            case LISTED, SUSPENDED -> DelistingStage.NORMAL;
            case DELISTING_RISK -> DelistingStage.WARNING;
            case DELISTING_NOTICE -> DelistingStage.DELISTING_NOTICE;
            case DELISTING_PROCESS -> DelistingStage.DELISTING_PROCESS;
            case DELISTED -> DelistingStage.DELISTED;
        };
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
            
            // ★ 순차 검증 및 자동 설정
            DelistingStage currentStage = validateAndAutoSetDelistingStage(
                stock.getDelistingStage(), DelistingStage.DELISTING_NOTICE, stock);
            
            stock.updateStatus(Stock.Status.DELISTING_NOTICE);
            stock.setDelistingNoticeDate(LocalDateTime.now());
            stock.setDelistingStage(DelistingStage.DELISTING_NOTICE);
            stockRepo.save(stock);
            
            log.info("상장폐지 예고 완료: stockId={}, stage: {} → DELISTING_NOTICE", stockId, currentStage);
            
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
     * DELISTING_NOTICE → DELISTING_PROCESS
     */
    @Transactional
    public void startDelistingProcess(UUID stockId) {
        log.info("상장폐지 절차 시작: stockId={}", stockId);
        
        try {
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            
            // ★ 순차 검증 및 자동 설정
            DelistingStage currentStage = validateAndAutoSetDelistingStage(
                stock.getDelistingStage(), DelistingStage.DELISTING_PROCESS, stock);
            
            stock.updateStatus(Stock.Status.DELISTING_PROCESS);
            stock.setDelistingStage(DelistingStage.DELISTING_PROCESS);
            stockRepo.save(stock);
            
            log.info("상장폐지 절차 시작 완료: stockId={}, stage: {} → DELISTING_PROCESS", stockId, currentStage);
            
            // 이력 기록
            recordHistory(stockId, ActionType.STAGE_CHANGE, 
                         currentStage,
                         DelistingStage.DELISTING_PROCESS,
                         "상장폐지 절차 시작 (환불 처리 시작)", null, null);
            
        } catch (Exception e) {
            log.error("상장폐지 절차 시작 실패: stockId={}", stockId, e);
            throw new RuntimeException("상장폐지 절차 시작 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 상장폐지 진행 상황 종합 체크 및 분석
     */
    public DelistingProgressResDto checkDelistingProgress(UUID stockId) {
        log.info("상장폐지 진행 상황 체크 시작: stockId={}", stockId);

        try {
            // 주식 정보 조회
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));

            // 해결되지 않은 위반 목록 조회
            List<DelistingViolation> unresolvedViolations = violationRepo.findByStockIdAndUnresolved(stockId);
            
            // 보상금 현황 조회
            List<DelistingCompensation> compensations = compensationRepo.findByStockId(stockId);
            
            // 최근 GPT 분석 결과 조회
            GptAnalysisResult latestAnalysis = gptAnalysisService.getLatestAnalysisResult(stockId);

            // 진행 단계 결정
            String progressStage = determineProgressStage(stock, unresolvedViolations);
            
            // 다음 단계 결정
            List<String> nextSteps = determineNextSteps(progressStage, unresolvedViolations);
            
            // 예상 상장폐지 일정 계산
            LocalDateTime estimatedDelistingDate = calculateEstimatedDelistingDate(progressStage, unresolvedViolations);
            
            // 전체 위험도 점수 계산
            BigDecimal overallRiskScore = calculateOverallRiskScore(unresolvedViolations, latestAnalysis);
            
            // 위험도 레벨 결정
            String riskLevel = determineRiskLevel(overallRiskScore);
            
            // 권장사항 생성
            List<String> recommendations = generateRecommendations(progressStage, unresolvedViolations, compensations);

            // 위반 요약 생성
            List<ViolationSummaryDto> violationSummaries = unresolvedViolations.stream()
                    .map(this::mapToViolationSummary)
                    .toList();

            // 보상금 현황 생성
            CompensationStatusDto compensationStatus = mapToCompensationStatus(compensations);

            DelistingProgressResDto result = DelistingProgressResDto.builder()
                    .stockId(stockId)
                    .ticker(stock.getTicker())
                    .nameKo(stock.getNameKo())
                    .currentStatus(stock.getStatus().toString())
                    .progressStage(progressStage)
                    .unresolvedViolations(violationSummaries)
                    .nextSteps(nextSteps)
                    .compensationStatus(compensationStatus)
                    .estimatedDelistingDate(estimatedDelistingDate)
                    .lastAnalysisDate(latestAnalysis != null ? latestAnalysis.getAnalysisDate() : null)
                    .totalViolationCount(unresolvedViolations.size())
                    .criticalViolationCount((int) unresolvedViolations.stream()
                            .filter(v -> v.getViolationType() == ViolationType.CRITICAL)
                            .count())
                    .overallRiskScore(overallRiskScore)
                    .riskLevel(riskLevel)
                    .recommendations(recommendations)
                    .build();

            log.info("상장폐지 진행 상황 체크 완료: stockId={}, stage={}, violations={}, riskLevel={}", 
                    stockId, progressStage, unresolvedViolations.size(), riskLevel);

            return result;

        } catch (Exception e) {
            log.error("상장폐지 진행 상황 체크 실패: stockId={}", stockId, e);
            throw new RuntimeException("상장폐지 진행 상황 체크 실패", e);
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
            // ✅ 기존 보상금 확인 (중복 방지)
            List<DelistingCompensation> existingCompensations = compensationRepo.findByStockId(stockId);
            if (!existingCompensations.isEmpty()) {
                log.info("이미 생성된 보상금이 존재: stockId={}, count={}", 
                        stockId, existingCompensations.size());
                
                // 모든 보상금이 완료되었는지 확인
                boolean allCompleted = existingCompensations.stream()
                    .allMatch(c -> c.getStatus() == CompensationStatus.COMPLETED);
                
                if (allCompleted) {
                    log.info("모든 보상금이 이미 완료됨: stockId={}", stockId);
                    return; // 재처리 불필요
                }
                
                log.info("미완료 보상금 존재 - 새로운 보상금 생성하지 않고 종료: stockId={}", stockId);
                return; // 기존 미완료 보상금 재처리는 retryFailedCompensations에서 처리
            }
            
            // stockId로 Stock 엔티티 조회하여 ticker 가져오기
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));

            String ticker = stock.getTicker();
            log.info("주식 정보 조회: stockId={}, ticker={}", stockId, ticker);

            // ordering 서비스에서 해당 주식의 모든 보유자 조회
            com.beyond.MKX.common.apiResponse.CommonDTO<List<StockHoldingResDto>> response = stockHoldingClient.getAllHoldersByTicker(ticker);
            
            if (response == null || response.getResult() == null) {
                log.info("보유자 정보 조회 실패: stockId={}, ticker={}", stockId, ticker);
                recordCompensationFailure(stockId, ticker, "HOLDINGS_NOT_FOUND", "보유자 정보를 찾을 수 없습니다");
                return;
            }
            
            List<StockHoldingResDto> holders = response.getResult();

            if (holders.isEmpty()) {
                log.info("보유자가 없음: stockId={}, ticker={}", stockId, ticker);
                return;
            }

            // 보상 기준 주가 조회 (marketdata 서비스에서 현재가 조회)
            BigDecimal compensationPrice;
            try {
                CommonDTO<CurrentPriceResDto> currentPriceResponse = currentPriceClient.getCurrentPrice(ticker);
                if (currentPriceResponse != null && currentPriceResponse.getResult() != null) {
                    CurrentPriceResDto currentPrice = currentPriceResponse.getResult();
                    if (currentPrice.price() != null) {
                        compensationPrice = BigDecimal.valueOf(currentPrice.price());
                        log.info("현재가 조회 성공: ticker={}, price={}", ticker, compensationPrice);
                    } else {
                        // 현재가 정보가 없으면 보상금 생성 중단하고 기록
                        log.error("현재가 정보 없음으로 보상금 생성 중단: stockId={}, ticker={}", stockId, ticker);
                        recordCompensationFailure(stockId, ticker, "CURRENT_PRICE_NOT_FOUND", "현재가 정보가 없습니다");
                        return;
                    }
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
            Map<UUID, String> accountNumberMap = new HashMap<>();
            for (StockHoldingResDto holder : holders) {
                BigDecimal compensationAmount = compensationPrice.multiply(
                    BigDecimal.valueOf(holder.totalQuantity())
                );

                // 계좌번호 조회 (null이면 예외 발생)
                String accountNumber = findAccountNumber(holder.memberAccountId());
                if (accountNumber == null) {
                    String errorMsg = String.format(
                        "계좌번호(account_number)를 찾을 수 없음: memberAccountId=%s, ticker=%s. " +
                        "계좌는 존재할 수 있으나 account_number 컬럼이 NULL이거나 비어있을 가능성 높음. " +
                        "DB 확인 필요: SELECT account_number FROM member_account WHERE id='%s'", 
                        holder.memberAccountId(), ticker, holder.memberAccountId()
                    );
                    log.error(errorMsg);
                    recordCompensationFailure(stockId, ticker, "ACCOUNT_NUMBER_NULL", errorMsg);
                    throw new IllegalStateException(errorMsg);
                }

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
                accountNumberMap.put(holder.memberAccountId(), accountNumber);
                totalCompensation = totalCompensation.add(compensationAmount);

                log.info("보상금 생성: accountId={}, accountNumber={}, quantity={}, amount={}",
                        holder.memberAccountId(), accountNumber, holder.totalQuantity(), compensationAmount);
            }

            // 실제 지급 처리 (3단계 방식)
            processCompensationPayment(stockId, compensations, totalCompensation, accountNumberMap);

            log.info("보상금 생성 및 지급 완료: stockId={}, 보유자 수={}, 총 보상금={}", 
                    stockId, holders.size(), totalCompensation);

        } catch (Exception e) {
            log.error("보상금 생성 중 오류 발생: stockId={}", stockId, e);
            // 전체 예외에 대해 실패 기록 남기기 (재처리 가능하도록)
            try {
                recordCompensationFailure(stockId, "COMPENSATION_CREATION_FAILED", 
                    "보상금 생성 중 전체 프로세스 실패: " + e.getMessage());
            } catch (Exception ex) {
                log.error("실패 기록 저장 중 오류: stockId={}", stockId, ex);
            }
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
                .executionDate(LocalDateTime.now())
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
                .executionDate(LocalDateTime.now())
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
        // ⭐ 먼저 전체 보상금 상태 확인 (delisting_compensation 테이블)
        List<DelistingCompensation> allCompensations = compensationRepo.findByStockId(stockId);
        log.info("전체 보상금: stockId={}, totalCount={}, status={}", stockId, allCompensations.size(),
                allCompensations.stream().map(c -> c.getStatus().toString()).toList());
        
        // 기존 미완료 보상금 조회 (FAILED, PENDING 등)
        List<DelistingCompensation> incompleteCompensations = allCompensations.stream()
                .filter(c -> c.getStatus() != CompensationStatus.COMPLETED)
                .toList();
        
        if (incompleteCompensations.isEmpty()) {
            log.info("재처리할 미완료 보상금 없음 - 이미 모두 완료됨: stockId={}", stockId);
            
            // history 기록도 업데이트
            List<DelistingHistory> failureRecords = historyRepo.findByStockIdAndActionTypeAndExecutionResult(
                    stockId, ActionType.COMPENSATION_FAILED, DelistingHistory.ExecutionResult.FAILED);
            if (!failureRecords.isEmpty()) {
                failureRecords.forEach(record -> {
                    record.setExecutionResult(DelistingHistory.ExecutionResult.SUCCESS);
                    record.setExecutionMessage("모든 보상금 처리 완료");
                });
                historyRepo.saveAll(failureRecords);
                log.info("실패 기록을 성공으로 표시 완료: stockId={}", stockId);
            }
            return;
        }
        
        log.info("미완료 보상금 재처리 시작: stockId={}, incompleteCount={}, status={}", 
                stockId, incompleteCompensations.size(),
                incompleteCompensations.stream().map(c -> c.getStatus().toString()).toList());
        
        // 계좌번호 매핑 생성 (null 체크 추가)
        Map<UUID, String> accountNumberMap = new HashMap<>();
        List<DelistingCompensation> validCompensations = new ArrayList<>();
        List<DelistingCompensation> invalidCompensations = new ArrayList<>();
        
        for (DelistingCompensation compensation : incompleteCompensations) {
            String accountNumber = findAccountNumber(compensation.getMemberAccountId());
            
            if (accountNumber == null) {
                log.error("❌ 재처리 실패: 계좌번호(account_number)를 찾을 수 없음 - memberAccountId={}, compensationId={}", 
                        compensation.getMemberAccountId(), compensation.getId());
                log.error("   계좌는 존재하지만 account_number가 NULL일 가능성 높음. SQL로 확인: SELECT account_number FROM member_account WHERE id='{}'", 
                        compensation.getMemberAccountId());
                
                // FAILED 상태로 업데이트 (이미 FAILED일 수도 있지만 확실히 설정)
                compensation.setStatus(CompensationStatus.FAILED);
                compensation.setFailureReason("계좌번호(account_number)가 NULL 또는 빈 문자열 - 데이터 무결성 문제");
                compensationRepo.save(compensation);
                
                invalidCompensations.add(compensation);
            } else {
                accountNumberMap.put(compensation.getMemberAccountId(), accountNumber);
                validCompensations.add(compensation);
                log.info("✅ 재처리 가능: memberAccountId={}, accountNumber={}, amount={}", 
                        compensation.getMemberAccountId(), accountNumber, compensation.getCompensationAmount());
            }
        }
        
        // 유효한 보상금이 없으면 종료
        if (validCompensations.isEmpty()) {
            log.error("❌ 재처리 가능한 보상금 없음 - 모든 보상금의 계좌번호를 찾을 수 없음: stockId={}", stockId);
            log.error("   총 {}개 보상금 중 {}개 모두 계좌번호(account_number) 조회 실패", incompleteCompensations.size(), invalidCompensations.size());
            log.error("   ⚠️ 데이터 무결성 문제: 계좌는 존재하지만 account_number 컬럼이 NULL이거나 비어있음");
            log.error("   해결 방법: 각 계좌에 account_number를 설정하거나, 계좌 생성 로직 수정 필요");
            
            String errorMsg = String.format(
                "재처리 실패: %d개 보상금 모두 계좌번호(account_number) 조회 실패 - account_number가 NULL", 
                incompleteCompensations.size()
            );
            recordCompensationFailure(stockId, "ACCOUNT_NUMBER_NULL_IN_RETRY", errorMsg);
            return;
        }
        
        // 일부만 유효한 경우 경고
        if (!invalidCompensations.isEmpty()) {
            log.warn("⚠️ 일부 보상금만 재처리 가능: valid={}, invalid={}", 
                    validCompensations.size(), invalidCompensations.size());
        }
        
        // 총 보상금 계산 (유효한 것만)
        BigDecimal totalCompensation = validCompensations.stream()
                .map(DelistingCompensation::getCompensationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        log.info("재처리할 보상금 합계: validCount={}, totalAmount={}", validCompensations.size(), totalCompensation);
        
        // 실제 지급 처리 (유효한 것만)
        processCompensationPayment(stockId, validCompensations, totalCompensation, accountNumberMap);
        
        log.info("✅ 보상금 재처리 지급 완료: stockId={}, validCount={}, invalidCount={}", 
                stockId, validCompensations.size(), invalidCompensations.size());
        
        // ⭐ 모든 보상금 처리 상태 확인
        List<DelistingCompensation> allCompensationsAfter = compensationRepo.findByStockId(stockId);
        boolean allCompleted = allCompensationsAfter.stream()
                .allMatch(c -> c.getStatus() == CompensationStatus.COMPLETED);
        
        if (allCompleted) {
            // ⭐ 모든 보상금이 완료되었으면 Stock Holdings 삭제
            try {
                Stock stock = stockRepo.findById(stockId)
                        .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
                String ticker = stock.getTicker();
                
                log.info("🗑️ 보상금 완료 후 Stock holdings 삭제 시작: ticker={}, stockId={}", ticker, stockId);
                
                CommonDTO<Integer> deleteResult = stockHoldingClient.deleteAllByTicker(ticker);
                
                if (deleteResult != null && deleteResult.getResult() != null) {
                    int deletedCount = deleteResult.getResult();
                    log.info("✅ Stock holdings 삭제 완료 (재처리): ticker={}, deletedCount={}", ticker, deletedCount);
                    
                    // 삭제 내역을 이력에 기록
                    recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                                 "Stock holdings 삭제 완료 (재처리): " + deletedCount + "건", null, null);
                } else {
                    log.warn("Stock holdings 삭제 결과가 null (재처리): ticker={}", ticker);
                }
                
                // 실패 기록을 성공으로 업데이트
                List<DelistingHistory> failureRecords = historyRepo.findByStockIdAndActionTypeAndExecutionResult(
                        stockId, ActionType.COMPENSATION_FAILED, DelistingHistory.ExecutionResult.FAILED);
                if (!failureRecords.isEmpty()) {
                    failureRecords.forEach(record -> {
                        record.setExecutionResult(DelistingHistory.ExecutionResult.SUCCESS);
                        record.setExecutionMessage("재처리 완료 - 모든 보상금 지급 및 stock holdings 삭제 완료");
                    });
                    historyRepo.saveAll(failureRecords);
                    log.info("실패 기록을 성공으로 업데이트: stockId={}, recordCount={}", stockId, failureRecords.size());
                }
                
            } catch (Exception e) {
                log.error("❌ Stock holdings 삭제 실패 (재처리): stockId={}, error={}", stockId, e.getMessage(), e);
                // 삭제 실패해도 보상금은 완료되었으므로 계속 진행
            }
        } else {
            log.info("⏳ 일부 보상금이 아직 미완료 상태 - Stock holdings는 나중에 삭제됨: stockId={}", stockId);
        }
        
        log.info("보상금 재처리 완료: stockId={}, validCount={}, invalidCount={}", 
                stockId, validCompensations.size(), invalidCompensations.size());
        
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
        
        // ★ 순차 검증 및 자동 설정
        DelistingStage currentStage = validateAndAutoSetDelistingStage(
            stock.getDelistingStage(), DelistingStage.DELISTED, stock);
        
        // ★ GPT 분석 결과에서 reason 가져오기
        DelistingReason delistingReason = extractAndMapDelistingReason(stockId);
        
        // 주식 상태를 DELISTED로 변경
        stock.updateStatus(Stock.Status.DELISTED);
        stock.setDelistingExecutionDate(LocalDateTime.now());
        stock.setDelistingStage(DelistingStage.DELISTED);
        stock.setDelistingReason(delistingReason);
        stockRepo.save(stock);
        
        log.info("주식 상태 변경 완료: stockId={}, stage: {} → DELISTED, reason={}", 
                stockId, currentStage, delistingReason);
        
        // 보상금 생성 및 지급 (MSA 연동)
        try {
            createCompensations(stockId);
            log.info("✅ 보상금 생성 및 지급 성공: stockId={}", stockId);
        } catch (Exception e) {
            log.error("❌ 보상금 생성 및 지급 실패: stockId={}, error={}", stockId, e.getMessage(), e);
            
            // 실패 이력 기록
            recordHistory(stockId, ActionType.COMPENSATION_FAILED, currentStage, null,
                         "보상금 처리 실패: " + e.getMessage(), null, null);
            
            // 보상금 실패 시 상장폐지를 롤백하지 않고 예외를 throw
            throw new RuntimeException("보상금 처리 실패: " + e.getMessage(), e);
        }
        
        // ✅ Stock Holdings 삭제 (환불 완료 후)
        try {
            String ticker = stock.getTicker();
            log.info("Stock holdings 삭제 시작: ticker={}, stockId={}", ticker, stockId);
            
            CommonDTO<Integer> deleteResult = stockHoldingClient.deleteAllByTicker(ticker);
            
            if (deleteResult != null && deleteResult.getResult() != null) {
                int deletedCount = deleteResult.getResult();
                log.info("✅ Stock holdings 삭제 완료: ticker={}, deletedCount={}", ticker, deletedCount);
                
                // 삭제 내역을 이력에 기록
                recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                             "Stock holdings 삭제 완료: " + deletedCount + "건", null, null);
            } else {
                log.warn("Stock holdings 삭제 결과가 null: ticker={}", ticker);
            }
        } catch (Exception e) {
            log.error("❌ Stock holdings 삭제 실패: stockId={}, ticker={}, error={}", 
                    stockId, stock.getTicker(), e.getMessage(), e);
            
            // 삭제 실패 이력 기록
            recordHistory(stockId, ActionType.DELISTING_EXECUTION, null, null,
                         "Stock holdings 삭제 실패: " + e.getMessage(), null, null);
            
            // 삭제 실패 시에도 상장폐지는 완료로 처리 (보상 작업으로 나중에 처리)
            // throw하지 않고 로그만 남김
        }
        
        // 이력 기록
        recordHistory(stockId, ActionType.DELISTING_EXECUTION, currentStage, DelistingStage.DELISTED,
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

// 스케줄러 중복 실행 방지를 위한 락
private final AtomicBoolean isProcessingRetry = new AtomicBoolean(false);

/**
 * 실패한 보상금 자동 재처리 스케줄러
 * 매 30초마다 실행되어 실패한 보상금을 자동으로 재시도
 */
@Scheduled(fixedRate = 30000) // 30초마다 실행 (30,000ms = 30초)
public void scheduledRetryFailedCompensations() {
    // 이미 실행 중이면 스킵 (중복 실행 방지)
    if (!isProcessingRetry.compareAndSet(false, true)) {
        log.info("⏭️ 실패한 보상금 재처리가 이미 실행 중 - 이번 스케줄 스킵");
        return;
    }
    
    try {
        log.info("실패한 보상금 자동 재처리 스케줄러 시작");
        
        // ⭐ delisting_compensation 테이블에서 FAILED 상태인 보상 조회
        List<DelistingCompensation> failedCompensations = compensationRepo.findByStatus(CompensationStatus.FAILED);
        
        if (failedCompensations.isEmpty()) {
            log.info("재처리할 실패 보상 없음 - 모든 보상금이 완료되었거나 진행 중이 아닙니다");
            return;
        }
        
        // 주식별로 그룹화하여 중복 처리 방지
        List<UUID> uniqueStockIds = failedCompensations.stream()
                .map(DelistingCompensation::getStockId)
                .distinct()
                .toList();
        
        log.info("실패한 보상금 재처리 대상: 보상금 {}건, 주식 {}개, stockIds={}", 
                failedCompensations.size(), uniqueStockIds.size(), uniqueStockIds);
        
        int successStockCount = 0;
        int failureStockCount = 0;
        int totalProcessedCompensations = 0;
        
        for (UUID stockId : uniqueStockIds) {
            try {
                // 해당 주식의 FAILED 보상금 개수 카운트
                long compensationCount = failedCompensations.stream()
                        .filter(c -> c.getStockId().equals(stockId))
                        .count();
                
                retryFailedCompensations(stockId);
                successStockCount++;
                totalProcessedCompensations += compensationCount;
                log.info("✅ 보상금 재처리 성공: stockId={}, 보상금 {}건", stockId, compensationCount);
            } catch (Exception e) {
                failureStockCount++;
                log.error("❌ 보상금 재처리 실패: stockId={}", stockId, e);
            }
        }
        
        log.info("실패한 보상금 자동 재처리 완료: 주식 성공 {}개 / 실패 {}개 / 전체 {}개, 보상금 총 {}건 처리", 
                successStockCount, failureStockCount, uniqueStockIds.size(), totalProcessedCompensations);
        
    } catch (Exception e) {
        log.error("실패한 보상금 자동 재처리 스케줄러 오류", e);
    } finally {
        // 락 해제
        isProcessingRetry.set(false);
        log.debug("스케줄러 락 해제");
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
     * 공시 승인 후 재평가: 미해결 위반 해지 처리 및 상태 정상화
     */
    @Transactional
    public void onDisclosureApproved(UUID stockId, UUID adminId) {
        log.info("공시 승인 후 재평가 수행: stockId={}", stockId);

        // 1) 미해결 위반 모두 해지 처리 (근거: 최신 공시 반영)
        List<DelistingViolation> unresolved = violationRepo.findByStockIdAndUnresolved(stockId);
        if (!unresolved.isEmpty()) {
            for (DelistingViolation v : unresolved) {
                v.setIsResolved(true);
                v.setResolvedDate(java.time.LocalDateTime.now());
                v.setResolvedBy(adminId);
                String prev = v.getDescription() != null ? v.getDescription() + "\n" : "";
                v.setDescription(prev + "[AUTO] 공시 승인 반영: 위반 해지");
                violationRepo.save(v);
            }
            // 위반 해지 이력
            String ids = unresolved.stream().map(x -> x.getId().toString()).reduce((a,b)->a+","+b).orElse("");
            recordHistory(stockId, ActionType.CRITERIA_VIOLATION, null, null,
                "공시 승인 반영으로 미해결 위반 해지", ids, adminId);
        }

        // 2) 주식 상태 정상화
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
        DelistingStage fromStage = stock.getDelistingStage();
        Stock.Status prevStatus = stock.getStatus();
        stock.updateStatus(Stock.Status.LISTED);
        stock.setDelistingStage(DelistingStage.NORMAL);
        stockRepo.save(stock);

        log.info("주식 상태 정상화: stockId={}, {}→LISTED, stage {}→NORMAL", stockId, prevStatus, fromStage);
        recordHistory(stockId, ActionType.STAGE_CHANGE, fromStage, DelistingStage.NORMAL,
            "공시 승인 반영으로 상태 정상화", null, adminId);
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
            
            // 재무 데이터 맵 생성 (GPT 분석용)
            Map<String, Object> financialData = buildFinancialDataMap(recentFinancials, recentRatios);
            
            for (DelistingCriteria criteria : activeCriteria) {
                // 각 기준별로 위반 여부 체크
                if (checkCriteriaViolationFromFinancials(stockId, criteria, recentFinancials, recentRatios, corporation)) {
                    violationCount++;
                }
                
                // 모든 기준에 대해 GPT 분석 실행 (위반 여부와 관계없이)
                performGptAnalysisForCriteria(stockId, criteria, financialData);
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
        
        // GPT 분석 결과 저장용 변수들
        BigDecimal gptRiskScore = null;
        String gptAnalysisDescription = null;
        String gptAnalysisReasoning = null;
        boolean gptAnalysisUsed = false;
        
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
                    // 자본금 기준 - CompanyFinancials의 totalEquity 사용 (실제 총자본)
                    if (!financials.isEmpty() && financials.get(0).getTotalEquity() != null) {
                        currentValue = BigDecimal.valueOf(financials.get(0).getTotalEquity());
                        description = String.format("총자본: %,.0f원", currentValue);
                        log.info("INSUFFICIENT_CAPITAL 체크 - 재무제표 데이터 사용: totalEquity={}, currentValue={}", 
                                financials.get(0).getTotalEquity(), currentValue);
                    } else {
                        // 재무제표 데이터가 없으면 Corporation의 자본금 사용
                        currentValue = BigDecimal.valueOf(corporation.getCapital());
                        description = String.format("설립자본금: %,.0f원", currentValue);
                        log.info("INSUFFICIENT_CAPITAL 체크 - Corporation 데이터 사용: capital={}, currentValue={}", 
                                corporation.getCapital(), currentValue);
                    }
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
                    
                // 재무제표로 감지 불가능한 기준들 (위반 체크에서 제외)
                case "FINAL_BANKRUPTCY":
                case "BREACH_OF_TRUST":
                case "EMBEZZLEMENT":
                case "REPORT_DELAY":
                    // 재무제표로 감지 불가능한 기준들은 위반 체크에서 제외
                    log.info("재무제표 기반 감지 불가능한 기준 건너뜀: {}", criteria.getCriteriaCode());
                    return false; // 위반 체크하지 않고 정상 처리
                    
                // 거래량 관련 기준들 (기본 틀만 구현, 나중에 marketData 연동)
                case "LOW_TRADING_VOLUME_2Q":
                    // TODO: marketData에서 실제 거래량 데이터 가져와서 구현
                    log.info("거래량 기준 - marketData 연동 예정: {}", criteria.getCriteriaCode());
                    return false; // 임시로 정상 처리
                    
                // 주가 관련 기준들 (기본 틀만 구현, 나중에 marketData 연동)
                case "LOW_STOCK_PRICE_30D":
                    // TODO: marketData에서 실제 주가 데이터 가져와서 구현
                    log.info("주가 기준 - marketData 연동 예정: {}", criteria.getCriteriaCode());
                    return false; // 임시로 정상 처리
                    
                // 시가총액 관련 기준들 (재무제표 기반 간접 추정)
                case "LOW_MARKET_CAP_CONTINUOUS":
                    // 재무제표로 간접 추정: 자산총계, 자기자본 등을 활용
                    if (!financials.isEmpty()) {
                        BigDecimal totalAssets = BigDecimal.valueOf(financials.get(0).getTotalAssets());
                        BigDecimal totalEquity = BigDecimal.valueOf(financials.get(0).getTotalEquity());
                        // 자산 대비 자기자본 비율이 낮으면 시가총액도 낮을 가능성
                        if (totalAssets.compareTo(BigDecimal.ZERO) > 0) {
                            currentValue = totalEquity.divide(totalAssets, 4, java.math.RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100));
                            description = String.format("자기자본비율: %.2f%% (시가총액 간접 추정)", currentValue);
                        }
                    }
                    break;
                    
                // 소수주주 관련 기준들 (재무제표 기반 간접 추정)
                case "LOW_MINORITY_SHAREHOLDERS":
                    // 재무제표로 간접 추정: 소액주주 배당금, 소액주주 이익 등을 활용
                    if (!financials.isEmpty()) {
                        BigDecimal netIncome = BigDecimal.valueOf(financials.get(0).getNetIncome());
                        // 순이익이 낮으면 소수주주 이익도 낮을 가능성
                        currentValue = netIncome;
                        description = String.format("순이익: %,.0f원 (소수주주 이익 간접 추정)", netIncome);
                    }
                    break;
                    
                // 감사 의견 관련 기준들 (GPT API 연동)
                case "AUDIT_OPINION_QUALIFIED":
                case "AUDIT_OPINION_ADVERSE":
                case "AUDIT_OPINION_DISCLAIMER":
                    // 재무제표 데이터를 GPT API로 분석
                    Map<String, Object> financialData = buildFinancialDataMap(financials, ratios);
                    OpenAiService.AuditOpinionAnalysis analysis = openAiService.analyzeAuditOpinionRisk(financialData);
                    
                    currentValue = analysis.getRiskScore();
                    description = String.format("GPT 재무분석 결과: %s (위험도: %.1f/10)", 
                            analysis.getDescription(), analysis.getRiskScore());
                    
                    // GPT 분석 결과 저장
                    gptRiskScore = analysis.getRiskScore();
                    gptAnalysisDescription = analysis.getDescription();
                    gptAnalysisReasoning = analysis.getReasoning();
                    gptAnalysisUsed = true;
                    
                    log.info("GPT 감사의견 분석 완료: criteria={}, riskScore={}, reasoning={}", 
                            criteria.getCriteriaCode(), analysis.getRiskScore(), analysis.getReasoning());
                    break;
                    
                default:
                    log.warn("알 수 없는 기준 코드: {}", criteria.getCriteriaCode());
                    return false;
            }
            
            if (currentValue != null && isViolation(criteria, currentValue)) {
                // 위반 감지 시 자동 기록 (GPT 분석 결과 포함)
                DelistingViolation violation = detectViolationWithGptAnalysis(stockId, criteria.getCriteriaCode(), 
                                                             currentValue, description, gptRiskScore, 
                                                             gptAnalysisDescription, gptAnalysisReasoning, gptAnalysisUsed);
                
                if (violation != null) {
                    log.info("재무제표 기반 위반 감지: stockId={}, criteria={}, value={}, gptUsed={}", 
                            stockId, criteria.getCriteriaCode(), currentValue, gptAnalysisUsed);
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
     * 보상금 실제 지급 처리
     * 
     * 프로세스:
     * 1. 기업 계좌 잔액 확인 (CorporationAccount)
     * 2. 기업 유동자산 확인 (Corporation.capital)
     * 3. 보상금 총액 대비 기업이 지급 가능한 금액 계산
     * 4. 기업이 가진 금액을 거래소에 먼저 입금
     * 5. 부족한 금액은 거래소가 funding으로 지급 (ExchangeSupportFund 생성)
     * 6. 거래소가 소유자들에게 일괄 환불 (기업/회원 모두 가능)
     */
    @Transactional
    private void processCompensationPayment(UUID stockId, List<DelistingCompensation> compensations, BigDecimal totalCompensation, Map<UUID, String> accountNumberMap) {
        log.info("보상금 지급 처리 시작: stockId={}, 총 보상금={}", stockId, totalCompensation);

        try {
            // 1. stock.corporationId로 기업 계좌 확인
            Stock stock = stockRepo.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
            UUID corporationId = stock.getCorporationId();
            
            CorporationAccount corpAccount = corporationAccountRepo.findByCorporationId(corporationId)
                    .orElseThrow(() -> new IllegalArgumentException("기업 계좌를 찾을 수 없음: corporationId=" + corporationId));
            
            // 2. 기업이 가지고 있는 금액 확인 (계좌 잔액)
            BigDecimal accountBalance = new BigDecimal(corpAccount.getBalance());
            log.info("기업 계좌 잔액: corporationId={}, balance={}", corporationId, accountBalance);
            
            // 3. Corporation의 capital 확인
            Corporation corporation = corporationRepo.findById(corporationId)
                    .orElseThrow(() -> new IllegalArgumentException("Corporation not found: " + corporationId));
            BigDecimal capital = new BigDecimal(corporation.getCapital());
            log.info("기업 자본금: corporationId={}, capital={}", corporationId, capital);
            
            // 기업이 지급 가능한 총액 = 계좌 잔액 + 자본금
            BigDecimal availableFromCorporation = accountBalance.add(capital);
            log.info("기업이 지급 가능한 총액: available={}, balance={}, capital={}", availableFromCorporation, accountBalance, capital);
            
            // 4. 보상금 총액과 비교
            if (availableFromCorporation.compareTo(totalCompensation) >= 0) {
                // 기업이 충분히 지급 가능
                log.info("기업이 충분히 지급 가능: available={}, totalCompensation={}", availableFromCorporation, totalCompensation);
                processFullPaymentFromCorporation(stockId, compensations, totalCompensation, accountNumberMap, corpAccount, corporation);
                
            } else {
                // 기업이 부족 → 기업이 가진 금액만큼 거래소에 입금하고, 부족분은 거래소가 funding
                BigDecimal shortage = totalCompensation.subtract(availableFromCorporation);
                log.info("기업이 부족함 - 거래소 funding 필요: shortage={}", shortage);
                
                // 기업 → 거래소 입금 (계좌 잔액 + 자본금 모두 거래소로)
                // 1) 계좌 잔액 전액 출금
                corpAccount.withdraw(accountBalance.toBigInteger());
                corporationAccountRepo.save(corpAccount);
                
                // 2) 자본금 전액 차감
                corporation.setCapital(0L); // 전체 자본금 차감
                corporationRepo.save(corporation);
                
                // 거래소 계좌에 입금
                ExchangeAccount exchangeAccount = getExchangeAccount();
                exchangeAccount.deposit(availableFromCorporation.toBigInteger());
                exchangeAccountRepo.save(exchangeAccount);
                
                log.info("기업 → 거래소 입금 완료: amount={}, accountBalance={}, capital={}", 
                        availableFromCorporation, accountBalance, capital);
                
                // 부족분은 거래소가 funding
                processExchangeFunding(stockId, corporationId, shortage);
                
                // 거래소가 일괄 환불 처리
                processRefundFromExchange(stockId, compensations, totalCompensation, accountNumberMap);
            }

        } catch (Exception e) {
            log.error("보상금 지급 처리 중 오류: stockId={}", stockId, e);
            recordCompensationFailure(stockId, "PAYMENT_PROCESSING_FAILED", "지급 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 기업이 충분한 경우 직접 환불 처리
     */
    private void processFullPaymentFromCorporation(UUID stockId, List<DelistingCompensation> compensations, 
                                                   BigDecimal totalCompensation, Map<UUID, String> accountNumberMap, 
                                                   CorporationAccount corpAccount, Corporation corporation) {
        log.info("기업이 직접 환불 처리: stockId={}", stockId);
        
        // 주식 정보 조회 (ticker 가져오기)
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
        String ticker = stock.getTicker();
        
        BigDecimal accountBalance = new BigDecimal(corpAccount.getBalance());
        
        if (accountBalance.compareTo(totalCompensation) >= 0) {
            // 계좌 잔액만으로 충분
            corpAccount.withdraw(totalCompensation.toBigInteger());
            corporationAccountRepo.save(corpAccount);
            log.info("계좌 잔액에서 출금: amount={}", totalCompensation);
            
        } else {
            // 계좌 잔액 전액 출금
            corpAccount.withdraw(accountBalance.toBigInteger());
            corporationAccountRepo.save(corpAccount);
            
            // 부족분은 자본금에서 차감
            BigDecimal remaining = totalCompensation.subtract(accountBalance);
            BigDecimal capital = new BigDecimal(corporation.getCapital());
            corporation.setCapital(capital.subtract(remaining).longValue());
            corporationRepo.save(corporation);
            log.info("계좌 잔액 전액 출금: {}, 자본금 차감: {}", accountBalance, remaining);
        }
        
        // 각 소유자에게 환불
        for (DelistingCompensation compensation : compensations) {
            processRefundToHolder(compensation, accountNumberMap, stockId, ticker);
        }
        
        log.info("기업 직접 환불 완료: stockId={}, totalCompensation={}", stockId, totalCompensation);
    }

    /**
     * 거래소 funding 처리 (ExchangeSupportFund 생성)
     */
    private void processExchangeFunding(UUID stockId, UUID corporationId, BigDecimal shortage) {
        log.info("거래소 funding 처리 시작: stockId={}, shortage={}", stockId, shortage);
        
        // 거래소 계좌에서 출금
        ExchangeAccount exchangeAccount = getExchangeAccount();
        exchangeAccount.withdraw(shortage.toBigInteger());
        exchangeAccountRepo.save(exchangeAccount);
        
        // ExchangeSupportFund 레코드 생성
        ExchangeSupportFund supportFund = ExchangeSupportFund.builder()
                .stockId(stockId)
                .corporationId(corporationId)
                .supportAmount(shortage)
                .supportType(ExchangeSupportFund.SupportType.COMPENSATION_LOAN)
                .status(ExchangeSupportFund.SupportStatus.ACTIVE)
                .reason("상장폐지 보상금 지원 - 기업 자금 부족")
                .repaymentDueDate(LocalDateTime.now().plusYears(3))
                .interestRate(new BigDecimal("5.0"))
                .processedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .processedAt(LocalDateTime.now())
                .remarks("기업이 충분한 자금을 조달하지 못하여 거래소에서 지원")
                .build();
        
        exchangeSupportFundRepo.save(supportFund);
        log.info("거래소 funding 기록 생성 완료: stockId={}, fundId={}, amount={}", 
                stockId, supportFund.getId(), shortage);
    }

    /**
     * 거래소에서 일괄 환불 처리
     */
    private void processRefundFromExchange(UUID stockId, List<DelistingCompensation> compensations, 
                                          BigDecimal totalCompensation, Map<UUID, String> accountNumberMap) {
        log.info("거래소 일괄 환불 처리 시작: stockId={}, totalCompensation={}", stockId, totalCompensation);
        
        // 주식 정보 조회 (ticker 가져오기)
        Stock stock = stockRepo.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("Stock not found: " + stockId));
        String ticker = stock.getTicker();
        
        // 각 소유자에게 환불
        for (DelistingCompensation compensation : compensations) {
            processRefundToHolder(compensation, accountNumberMap, stockId, ticker);
        }
        
        log.info("거래소 일괄 환불 완료: stockId={}, totalCompensation={}", stockId, totalCompensation);
    }

    /**
     * 소유자에게 환불 처리 (기업/회원 구분)
     */
    private void processRefundToHolder(DelistingCompensation compensation, Map<UUID, String> accountNumberMap, 
                                       UUID stockId, String ticker) {
        try {
            String accountNumber = accountNumberMap.get(compensation.getMemberAccountId());
            if (accountNumber == null) {
                log.error("계좌번호를 찾을 수 없음: memberAccountId={}", compensation.getMemberAccountId());
                compensation.setStatus(CompensationStatus.FAILED);
                compensationRepo.save(compensation);
                return;
            }
            
            // AccountList 조회하여 계좌 타입 확인
            AccountList accountList = accountListRepo.findByAccountNumber(accountNumber)
                    .orElse(null);
            
            if (accountList == null) {
                log.error("계좌 정보를 찾을 수 없음: accountNumber={}", accountNumber);
                compensation.setStatus(CompensationStatus.FAILED);
                compensationRepo.save(compensation);
                return;
            }
            
            // 계좌 타입에 따라 다른 처리
            if (accountList.getType() == AccountType.MEMBER) {
                // MEMBER 계좌: ordering-service를 통해 입금
                MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                        compensation.getCompensationAmount().longValue()
                );
                
                try {
                    Map<String, Object> response = memberAccountClient.depositByAccountNumber(accountNumber, request);
                    
                    // ordering 서비스는 CommonDTO 형식으로 응답: { result: { success, message, newBalance } }
                    boolean success = false;
                    String message = "알 수 없는 오류";
                    
                    if (response != null) {
                        Object resultObj = response.get("result");
                        if (resultObj instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> result = (Map<String, Object>) resultObj;
                            success = Boolean.TRUE.equals(result.get("success"));
                            message = (String) result.get("message");
                        }
                    }
                    
                    if (success) {
                        compensation.setStatus(CompensationStatus.COMPLETED);
                        compensation.setProcessedAt(LocalDateTime.now());
                        compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                        compensationRepo.save(compensation);
                        
                        // ✅ Kafka 이벤트 발행
                        publishDelistingRefundEvent(
                            accountNumber,
                            compensation.getMemberAccountId().toString(),
                            "MEMBER",
                            compensation.getCompensationAmount().longValue(),
                            stockId,
                            ticker,
                            compensation.getStockQuantity().longValue(),  // 주식 수량
                            compensation.getCompensationPrice().longValue()  // 주당 가격
                        );
                        
                        log.info("✅ 회원 계좌 환불 완료 및 이벤트 발행: accountNumber={}, amount={}, ticker={}", 
                                accountNumber, compensation.getCompensationAmount(), ticker);
                    } else {
                        log.error("❌ 회원 계좌 환불 실패: accountNumber={}, message={}", accountNumber, message);
                        compensation.setStatus(CompensationStatus.FAILED);
                        compensation.setFailureReason("입금 실패: " + message);
                        compensationRepo.save(compensation);
                    }
                } catch (Exception e) {
                    log.error("❌ 회원 계좌 환불 중 예외 발생: accountNumber={}, error={}", accountNumber, e.getMessage(), e);
                    compensation.setStatus(CompensationStatus.FAILED);
                    compensation.setFailureReason("입금 중 예외: " + e.getMessage());
                    compensationRepo.save(compensation);
                }
                
            } else if (accountList.getType() == AccountType.CORPORATION) {
                // CORPORATION 계좌: mkx-platform 내부에서 처리
                CorporationAccount targetCorpAccount = corporationAccountRepo.findByAccountNumber(accountNumber)
                        .orElse(null);
                
                if (targetCorpAccount != null) {
                    targetCorpAccount.deposit(compensation.getCompensationAmount().toBigInteger());
                    corporationAccountRepo.save(targetCorpAccount);
                    compensation.setStatus(CompensationStatus.COMPLETED);
                    compensation.setProcessedAt(LocalDateTime.now());
                    compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                    compensationRepo.save(compensation);
                    
                    // ✅ Kafka 이벤트 발행
                    publishDelistingRefundEvent(
                        accountNumber,
                        targetCorpAccount.getId().toString(),
                        "CORPORATION",
                        compensation.getCompensationAmount().longValue(),
                        stockId,
                        ticker,
                        compensation.getStockQuantity().longValue(),  // 주식 수량
                        compensation.getCompensationPrice().longValue()  // 주당 가격
                    );
                    
                    log.info("기업 계좌 환불 완료 및 이벤트 발행: accountNumber={}, amount={}, ticker={}", 
                            accountNumber, compensation.getCompensationAmount(), ticker);
                } else {
                    log.error("기업 계좌를 찾을 수 없음: accountNumber={}", accountNumber);
                    compensation.setStatus(CompensationStatus.FAILED);
                    compensationRepo.save(compensation);
                }
            } else {
                log.error("지원하지 않는 계좌 타입: type={}, accountNumber={}", accountList.getType(), accountNumber);
                compensation.setStatus(CompensationStatus.FAILED);
                compensationRepo.save(compensation);
            }
            
        } catch (Exception e) {
            log.error("소유자 환불 처리 실패: memberAccountId={}", compensation.getMemberAccountId(), e);
            compensation.setStatus(CompensationStatus.FAILED);
            compensationRepo.save(compensation);
        }
    }

    /**
     * 1단계: 기업 계좌 현금으로 지급
     */
    private void processPaymentFromCash(UUID stockId, List<DelistingCompensation> compensations, BigDecimal totalCompensation, Map<UUID, String> accountNumberMap) {
        CorporationAccount corpAccount = getCorporationAccountByStockId(stockId);
        
        // 기업 계좌에서 출금
        corpAccount.withdraw(totalCompensation.toBigInteger());
        corporationAccountRepo.save(corpAccount);
        
        // 각 보상금 지급 처리
        for (DelistingCompensation compensation : compensations) {
            try {
                String accountNumber = accountNumberMap.get(compensation.getMemberAccountId());
                if (accountNumber == null) {
                    log.error("계좌번호를 찾을 수 없음: memberAccountId={}", compensation.getMemberAccountId());
                    compensation.setStatus(CompensationStatus.FAILED);
                    compensationRepo.save(compensation);
                    continue;
                }
                
                // AccountList 조회하여 계좌 타입 확인
                AccountList accountList = accountListRepo.findByAccountNumber(accountNumber)
                        .orElse(null);
                
                if (accountList == null) {
                    log.error("계좌 정보를 찾을 수 없음: accountNumber={}", accountNumber);
                    compensation.setStatus(CompensationStatus.FAILED);
                    compensationRepo.save(compensation);
                    continue;
                }
                
                // 계좌 타입에 따라 다른 처리
                boolean success = false;
                
                if (accountList.getType() == AccountType.MEMBER) {
                    // MEMBER 계좌: ordering-service를 통해 입금
                    MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                            compensation.getCompensationAmount().longValue()
                    );
                    try {
                        Map<String, Object> response = memberAccountClient.depositByAccountNumber(accountNumber, request);
                        if (response != null) {
                            Object resultObj = response.get("result");
                            if (resultObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> result = (Map<String, Object>) resultObj;
                                success = Boolean.TRUE.equals(result.get("success"));
                            }
                        }
                    } catch (Exception e) {
                        log.error("입금 중 예외: accountNumber={}, error={}", accountNumber, e.getMessage());
                        success = false;
                    }
                    
                } else if (accountList.getType() == AccountType.CORPORATION) {
                    // CORPORATION 계좌: mkx-platform 내부에서 처리
                    CorporationAccount targetCorpAccount = corporationAccountRepo.findByAccountNumber(accountNumber)
                            .orElse(null);
                    
                    if (targetCorpAccount != null) {
                        targetCorpAccount.deposit(compensation.getCompensationAmount().toBigInteger());
                        corporationAccountRepo.save(targetCorpAccount);
                        success = true;
                        log.info("기업 계좌 입금 완료: accountNumber={}, amount={}", accountNumber, compensation.getCompensationAmount());
                    } else {
                        log.error("기업 계좌를 찾을 수 없음: accountNumber={}", accountNumber);
                    }
                } else {
                    log.error("지원하지 않는 계좌 타입: type={}, accountNumber={}", accountList.getType(), accountNumber);
                }
                
                if (success) {
                    // 보상금 상태 업데이트
                    compensation.setStatus(CompensationStatus.COMPLETED);
                    compensation.setProcessedAt(LocalDateTime.now());
                    compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000")); // 시스템 처리
                    compensationRepo.save(compensation);
                    
                    log.info("현금 지급 완료: accountId={}, accountNumber={}, type={}, amount={}", 
                            compensation.getMemberAccountId(), accountNumber, accountList.getType(), compensation.getCompensationAmount());
                } else {
                    log.error("현금 지급 실패: accountId={}, accountNumber={}, type={}", 
                            compensation.getMemberAccountId(), accountNumber, accountList.getType());
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
                                                     BigDecimal totalCompensation, BigDecimal availableCash, Map<UUID, String> accountNumberMap) {
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
                    String accountNumber = accountNumberMap.get(compensation.getMemberAccountId());
                    if (accountNumber == null) {
                        log.error("계좌번호를 찾을 수 없음: memberAccountId={}", compensation.getMemberAccountId());
                        compensation.setStatus(CompensationStatus.FAILED);
                        compensationRepo.save(compensation);
                        continue;
                    }
                    
                    // AccountList 조회하여 계좌 타입 확인
                    AccountList accountList = accountListRepo.findByAccountNumber(accountNumber)
                            .orElse(null);
                    
                    if (accountList == null) {
                        log.error("계좌 정보를 찾을 수 없음: accountNumber={}", accountNumber);
                        compensation.setStatus(CompensationStatus.FAILED);
                        compensationRepo.save(compensation);
                        continue;
                    }
                    
                    // 계좌 타입에 따라 다른 처리
                    boolean success = false;
                    
                    if (accountList.getType() == AccountType.MEMBER) {
                        // MEMBER 계좌: ordering-service를 통해 입금
                        MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                                compensation.getCompensationAmount().longValue()
                        );
                        try {
                            Map<String, Object> response = memberAccountClient.depositByAccountNumber(accountNumber, request);
                            if (response != null) {
                                Object resultObj = response.get("result");
                                if (resultObj instanceof Map) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> result = (Map<String, Object>) resultObj;
                                    success = Boolean.TRUE.equals(result.get("success"));
                                }
                            }
                        } catch (Exception e) {
                            log.error("입금 중 예외: accountNumber={}, error={}", accountNumber, e.getMessage());
                            success = false;
                        }
                        
                    } else if (accountList.getType() == AccountType.CORPORATION) {
                        // CORPORATION 계좌: mkx-platform 내부에서 처리
                        CorporationAccount targetCorpAccount = corporationAccountRepo.findByAccountNumber(accountNumber)
                                .orElse(null);
                        
                        if (targetCorpAccount != null) {
                            targetCorpAccount.deposit(compensation.getCompensationAmount().toBigInteger());
                            corporationAccountRepo.save(targetCorpAccount);
                            success = true;
                            log.info("기업 계좌 입금 완료 (유동자산): accountNumber={}, amount={}", accountNumber, compensation.getCompensationAmount());
                        } else {
                            log.error("기업 계좌를 찾을 수 없음: accountNumber={}", accountNumber);
                        }
                    } else {
                        log.error("지원하지 않는 계좌 타입: type={}, accountNumber={}", accountList.getType(), accountNumber);
                    }
                    
                    if (success) {
                        compensation.setStatus(CompensationStatus.COMPLETED);
                        compensation.setProcessedAt(LocalDateTime.now());
                        compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                        compensationRepo.save(compensation);
                        
                        log.info("유동자산 지급 완료: accountId={}, accountNumber={}, type={}, amount={}", 
                                compensation.getMemberAccountId(), accountNumber, accountList.getType(), compensation.getCompensationAmount());
                    } else {
                        log.error("유동자산 지급 실패: accountId={}, accountNumber={}, type={}", 
                                compensation.getMemberAccountId(), accountNumber, accountList.getType());
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
                        String accountNumber = accountNumberMap.get(compensation.getMemberAccountId());
                        if (accountNumber == null) {
                            log.error("계좌번호를 찾을 수 없음: memberAccountId={}", compensation.getMemberAccountId());
                            compensation.setStatus(CompensationStatus.FAILED);
                            compensationRepo.save(compensation);
                            continue;
                        }
                        
                        // AccountList 조회하여 계좌 타입 확인
                        AccountList accountList = accountListRepo.findByAccountNumber(accountNumber)
                                .orElse(null);
                        
                        if (accountList == null) {
                            log.error("계좌 정보를 찾을 수 없음: accountNumber={}", accountNumber);
                            compensation.setStatus(CompensationStatus.FAILED);
                            compensationRepo.save(compensation);
                            continue;
                        }
                        
                        // 계좌 타입에 따라 다른 처리
                        boolean success = false;
                        
                        if (accountList.getType() == AccountType.MEMBER) {
                            MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                                    proportionalAmount.longValue()
                            );
                            try {
                                Map<String, Object> response = memberAccountClient.depositByAccountNumber(accountNumber, request);
                                if (response != null) {
                                    Object resultObj = response.get("result");
                                    if (resultObj instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> result = (Map<String, Object>) resultObj;
                                        success = Boolean.TRUE.equals(result.get("success"));
                                    }
                                }
                            } catch (Exception e) {
                                log.error("입금 중 예외: accountNumber={}, error={}", accountNumber, e.getMessage());
                                success = false;
                            }
                            
                        } else if (accountList.getType() == AccountType.CORPORATION) {
                            CorporationAccount targetCorpAccount = corporationAccountRepo.findByAccountNumber(accountNumber)
                                    .orElse(null);
                            
                            if (targetCorpAccount != null) {
                                targetCorpAccount.deposit(proportionalAmount.toBigInteger());
                                corporationAccountRepo.save(targetCorpAccount);
                                success = true;
                                log.info("기업 계좌 입금 완료 (부분 지급): accountNumber={}, amount={}", accountNumber, proportionalAmount);
                            } else {
                                log.error("기업 계좌를 찾을 수 없음: accountNumber={}", accountNumber);
                            }
                        } else {
                            log.error("지원하지 않는 계좌 타입: type={}, accountNumber={}", accountList.getType(), accountNumber);
                        }
                        
                        if (success) {
                            compensation.setStatus(CompensationStatus.PARTIAL_PAID);
                            compensation.setProcessedAt(LocalDateTime.now());
                            compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                            compensationRepo.save(compensation);
                        } else {
                            log.error("부분 지급 실패: accountId={}, type={}", compensation.getMemberAccountId(), accountList.getType());
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
    private void processPaymentFromExchange(UUID stockId, List<DelistingCompensation> compensations, BigDecimal remainingAmount, Map<UUID, String> accountNumberMap) {
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
                        String accountNumber = accountNumberMap.get(compensation.getMemberAccountId());
                        if (accountNumber == null) {
                            log.error("계좌번호를 찾을 수 없음: memberAccountId={}", compensation.getMemberAccountId());
                            compensation.setStatus(CompensationStatus.FAILED);
                            compensationRepo.save(compensation);
                            continue;
                        }
                        
                        // AccountList 조회하여 계좌 타입 확인
                        AccountList accountList = accountListRepo.findByAccountNumber(accountNumber)
                                .orElse(null);
                        
                        if (accountList == null) {
                            log.error("계좌 정보를 찾을 수 없음: accountNumber={}", accountNumber);
                            compensation.setStatus(CompensationStatus.FAILED);
                            compensationRepo.save(compensation);
                            continue;
                        }
                        
                        // 계좌 타입에 따라 다른 처리
                        boolean success = false;
                        
                        if (accountList.getType() == AccountType.MEMBER) {
                            MemberAccountClient.DepositRequest request = new MemberAccountClient.DepositRequest(
                                    unpaidAmount.longValue()
                            );
                            try {
                                Map<String, Object> response = memberAccountClient.depositByAccountNumber(accountNumber, request);
                                if (response != null) {
                                    Object resultObj = response.get("result");
                                    if (resultObj instanceof Map) {
                                        @SuppressWarnings("unchecked")
                                        Map<String, Object> result = (Map<String, Object>) resultObj;
                                        success = Boolean.TRUE.equals(result.get("success"));
                                    }
                                }
                            } catch (Exception e) {
                                log.error("입금 중 예외: accountNumber={}, error={}", accountNumber, e.getMessage());
                                success = false;
                            }
                            
                        } else if (accountList.getType() == AccountType.CORPORATION) {
                            CorporationAccount targetCorpAccount = corporationAccountRepo.findByAccountNumber(accountNumber)
                                    .orElse(null);
                            
                            if (targetCorpAccount != null) {
                                targetCorpAccount.deposit(unpaidAmount.toBigInteger());
                                corporationAccountRepo.save(targetCorpAccount);
                                success = true;
                                log.info("기업 계좌 입금 완료 (거래소 지급): accountNumber={}, amount={}", accountNumber, unpaidAmount);
                            } else {
                                log.error("기업 계좌를 찾을 수 없음: accountNumber={}", accountNumber);
                            }
                        } else {
                            log.error("지원하지 않는 계좌 타입: type={}, accountNumber={}", accountList.getType(), accountNumber);
                        }
                        
                        if (success) {
                            compensation.setStatus(CompensationStatus.COMPLETED);
                            compensation.setProcessedAt(LocalDateTime.now());
                            compensation.setProcessedBy(UUID.fromString("00000000-0000-0000-0000-000000000000"));
                            compensationRepo.save(compensation);
                        } else {
                            log.error("거래소 지급 실패: accountId={}, type={}", 
                                    compensation.getMemberAccountId(), accountList.getType());
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

    /**
     * GPT API 분석을 위한 재무 데이터 맵 생성 (null 값 필터링)
     */
    private Map<String, Object> buildFinancialDataMap(List<CompanyFinancials> financials, List<FinancialRatios> ratios) {
        Map<String, Object> data = new HashMap<>();
        
        if (!financials.isEmpty()) {
            CompanyFinancials latest = financials.get(0);
            
            // null이 아닌 값만 추가
            if (latest.getTotalAssets() != null && latest.getTotalAssets() > 0) {
                data.put("totalAssets", latest.getTotalAssets());
            }
            if (latest.getTotalLiabilities() != null && latest.getTotalLiabilities() > 0) {
                data.put("totalLiabilities", latest.getTotalLiabilities());
            }
            if (latest.getTotalEquity() != null && latest.getTotalEquity() > 0) {
                data.put("totalEquity", latest.getTotalEquity());
            }
            if (latest.getRevenue() != null && latest.getRevenue() > 0) {
                data.put("revenue", latest.getRevenue());
            }
            if (latest.getOperatingIncome() != null) {
                data.put("operatingIncome", latest.getOperatingIncome());
            }
            if (latest.getNetIncome() != null) {
                data.put("netIncome", latest.getNetIncome());
            }
        }
        
        if (!ratios.isEmpty()) {
            FinancialRatios latest = ratios.get(0);
            
            // null이 아닌 값만 추가
            if (latest.getCurrentRatio() != null && latest.getCurrentRatio().compareTo(BigDecimal.ZERO) > 0) {
                data.put("currentRatio", latest.getCurrentRatio());
            }
            if (latest.getDebtRatio() != null && latest.getDebtRatio().compareTo(BigDecimal.ZERO) > 0) {
                data.put("debtRatio", latest.getDebtRatio());
            }
            if (latest.getRoe() != null) {
                data.put("roe", latest.getRoe());
            }
            if (latest.getRoa() != null) {
                data.put("roa", latest.getRoa());
            }
        }
        
        log.info("GPT 분석용 재무 데이터 필터링 완료: availableFields={}", data.keySet());
        return data;
    }

    /**
     * 특정 기준에 대한 GPT 분석 실행
     */
    private void performGptAnalysisForCriteria(UUID stockId, DelistingCriteria criteria, Map<String, Object> financialData) {
        try {
            String analysisType = determineGptAnalysisType(criteria.getCriteriaCode());
            
            if (analysisType != null) {
                log.info("GPT 분석 실행: stockId={}, criteriaCode={}, analysisType={}", 
                        stockId, criteria.getCriteriaCode(), analysisType);
                
                gptAnalysisService.performAnalysis(stockId, criteria.getCriteriaCode(), analysisType, financialData);
            }
        } catch (Exception e) {
            log.error("GPT 분석 실행 실패: stockId={}, criteriaCode={}", stockId, criteria.getCriteriaCode(), e);
        }
    }

    /**
     * 기준 코드에 따른 GPT 분석 유형 결정
     */
    private String determineGptAnalysisType(String criteriaCode) {
        return switch (criteriaCode) {
            case "AUDIT_OPINION_QUALIFIED" -> "AUDIT_OPINION_QUALIFIED";
            case "AUDIT_OPINION_ADVERSE" -> "AUDIT_OPINION_ADVERSE";
            case "AUDIT_OPINION_DISCLAIMER" -> "AUDIT_OPINION_DISCLAIMER";
            case "LOW_REVENUE_2Y", "NEGATIVE_NET_INCOME", "LOW_EQUITY_RATIO", 
                 "HIGH_DEBT_RATIO", "LOW_CURRENT_RATIO", "INSUFFICIENT_CAPITAL",
                 "LOW_ROE", "LOW_ROA", "NEGATIVE_OPERATING_INCOME", "LOW_INTEREST_COVERAGE" -> "FINANCIAL_HEALTH";
            case "LOW_MARKET_CAP_CONTINUOUS", "LOW_MINORITY_SHAREHOLDERS" -> "RISK_ASSESSMENT";
            default -> null; // GPT 분석이 필요하지 않은 기준들
        };
    }

    /**
     * 진행 단계 결정
     */
    private String determineProgressStage(Stock stock, List<DelistingViolation> violations) {
        if (violations.isEmpty()) {
            return "NORMAL";
        }
        
        boolean hasCriticalViolations = violations.stream()
                .anyMatch(v -> v.getViolationType() == ViolationType.CRITICAL);
        
        if (hasCriticalViolations) {
            return "CRITICAL_VIOLATIONS";
        } else {
            return "WARNING_VIOLATIONS";
        }
    }

    /**
     * 다음 단계 결정
     */
    private List<String> determineNextSteps(String progressStage, List<DelistingViolation> violations) {
        return switch (progressStage) {
            case "NORMAL" -> List.of("정상 운영", "정기 모니터링");
            case "WARNING_VIOLATIONS" -> List.of("위반 기준 개선", "재무제표 정정", "정기 모니터링");
            case "CRITICAL_VIOLATIONS" -> List.of("상장폐지 예고 발행", "보상금 지급 처리", "상장폐지 절차 준비");
            default -> List.of("상황 분석 필요");
        };
    }

    /**
     * 예상 상장폐지 일정 계산
     */
    private LocalDateTime calculateEstimatedDelistingDate(String progressStage, List<DelistingViolation> violations) {
        return switch (progressStage) {
            case "CRITICAL_VIOLATIONS" -> LocalDateTime.now().plusMonths(2);
            case "WARNING_VIOLATIONS" -> LocalDateTime.now().plusMonths(6);
            default -> null;
        };
    }

    /**
     * 전체 위험도 점수 계산
     */
    private BigDecimal calculateOverallRiskScore(List<DelistingViolation> violations, GptAnalysisResult latestAnalysis) {
        BigDecimal violationScore = violations.stream()
                .map(v -> BigDecimal.valueOf(v.getSeverityScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal gptScore = latestAnalysis != null && latestAnalysis.getRiskScore() != null 
                ? latestAnalysis.getRiskScore() 
                : BigDecimal.ZERO;
        
        return violationScore.add(gptScore);
    }

    /**
     * 위험도 레벨 결정
     */
    private String determineRiskLevel(BigDecimal riskScore) {
        if (riskScore.compareTo(BigDecimal.valueOf(20)) >= 0) {
            return "CRITICAL";
        } else if (riskScore.compareTo(BigDecimal.valueOf(15)) >= 0) {
            return "HIGH";
        } else if (riskScore.compareTo(BigDecimal.valueOf(10)) >= 0) {
            return "MEDIUM";
        } else if (riskScore.compareTo(BigDecimal.valueOf(5)) >= 0) {
            return "LOW";
        } else {
            return "MINIMAL";
        }
    }

    /**
     * 권장사항 생성
     */
    private List<String> generateRecommendations(String progressStage, List<DelistingViolation> violations, List<DelistingCompensation> compensations) {
        List<String> recommendations = new ArrayList<>();
        
        if (progressStage.equals("CRITICAL_VIOLATIONS")) {
            recommendations.add("즉시 상장폐지 예고 발행 필요");
            recommendations.add("보상금 지급 절차 시작");
        }
        
        if (!violations.isEmpty()) {
            recommendations.add("위반 기준 개선 방안 수립");
        }
        
        boolean hasFailedCompensations = compensations.stream()
                .anyMatch(c -> c.getStatus() == CompensationStatus.FAILED);
        
        if (hasFailedCompensations) {
            recommendations.add("실패한 보상금 재처리 필요");
        }
        
        return recommendations.isEmpty() ? List.of("정상 운영 중") : recommendations;
    }

    /**
     * 위반 요약 매핑
     */
    private ViolationSummaryDto mapToViolationSummary(DelistingViolation violation) {
        return ViolationSummaryDto.builder()
                .criteriaCode(violation.getCriteriaCode())
                .criteriaName(violation.getCriteriaCode()) // TODO: 기준명 조회
                .violationDate(violation.getViolationDate())
                .consecutivePeriods(violation.getConsecutivePeriods())
                .severityScore(violation.getSeverityScore())
                .description(violation.getDescription())
                .gptAnalysisUsed(violation.getGptAnalysisUsed())
                .gptRiskScore(violation.getGptRiskScore())
                .build();
    }

    /**
     * 보상금 현황 매핑
     */
    private CompensationStatusDto mapToCompensationStatus(List<DelistingCompensation> compensations) {
        if (compensations.isEmpty()) {
            return CompensationStatusDto.builder()
                    .isCompleted(false)
                    .totalAmount(BigDecimal.ZERO)
                    .paidAmount(BigDecimal.ZERO)
                    .unpaidAmount(BigDecimal.ZERO)
                    .status("NOT_STARTED")
                    .build();
        }
        
        BigDecimal totalAmount = compensations.stream()
                .map(DelistingCompensation::getCompensationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal paidAmount = compensations.stream()
                .filter(c -> c.getStatus() == CompensationStatus.COMPLETED)
                .map(DelistingCompensation::getCompensationAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        boolean isCompleted = compensations.stream()
                .allMatch(c -> c.getStatus() == CompensationStatus.COMPLETED);
        
        return CompensationStatusDto.builder()
                .isCompleted(isCompleted)
                .totalAmount(totalAmount)
                .paidAmount(paidAmount)
                .unpaidAmount(totalAmount.subtract(paidAmount))
                .status(isCompleted ? "COMPLETED" : "IN_PROGRESS")
                .build();
    }
    
    /**
     * GPT 분석 결과에서 상장폐지 사유 추출 및 매핑
     */
    private DelistingReason extractAndMapDelistingReason(UUID stockId) {
        String gptReason = extractDelistingReasonFromGpt(stockId);
        return mapToDelistingReason(gptReason);
    }
    
    /**
     * GPT 분석 결과에서 reason 추출
     */
    private String extractDelistingReasonFromGpt(UUID stockId) {
        try {
            GptAnalysisResult latestAnalysis = gptAnalysisService.getLatestAnalysisResult(stockId);
            if (latestAnalysis != null && latestAnalysis.getAnalysisReasoning() != null) {
                return latestAnalysis.getAnalysisReasoning();
            }
        } catch (Exception e) {
            log.warn("GPT 분석 결과 조회 실패: stockId={}", stockId, e);
        }
        return null;
    }
    
    /**
     * String을 DelistingReason enum으로 매핑
     */
    private DelistingReason mapToDelistingReason(String gptReason) {
        if (gptReason == null) {
            return DelistingReason.FINANCIAL_DISTRESS;  // 기본값
        }
        
        String lowerReason = gptReason.toLowerCase();
        
        if (lowerReason.contains("재무") || lowerReason.contains("매출") || lowerReason.contains("순이익")) {
            return DelistingReason.FINANCIAL_DISTRESS;
        } else if (lowerReason.contains("거래량") || lowerReason.contains("거래")) {
            return DelistingReason.LOW_TRADING_VOLUME;
        } else if (lowerReason.contains("법규") || lowerReason.contains("위반")) {
            return DelistingReason.REGULATORY_VIOLATION;
        } else if (lowerReason.contains("제출") || lowerReason.contains("지연")) {
            return DelistingReason.REPORT_DELAY;
        } else if (lowerReason.contains("부도")) {
            return DelistingReason.BANKRUPTCY;
        } else {
            return DelistingReason.FINANCIAL_DISTRESS;  // 기본값
        }
    }
    
    /**
     * DELISTING_NOTICE 상태인 주식을 자동으로 DELISTED로 전환 (미사용 - 관리자가 수동 실행)
     * 흐름: DELISTING_NOTICE → DELISTED
     */
    @Transactional
    public void processAutoDelistingProcess() {
        // 자동 전환 제거: 관리자가 수동으로 executeDelisting 실행
        log.debug("상장폐지 절차는 관리자가 수동으로 실행해야 합니다");
    }
    
    /**
     * DELISTING_RISK 상태인 주식 중 3분 이상 지난 것을 자동으로 DELISTING_NOTICE로 전환
     * 흐름: WARNING → DELISTING_NOTICE → DELISTING_PROCESS → DELISTED
     */
    @Transactional
    public void processAutoDelisting() {
        try {
            // DELISTING_RISK 상태인 모든 주식 조회
            List<Stock> atRiskStocks = stockRepo.findByStatus(Stock.Status.DELISTING_RISK);
            
            if (atRiskStocks.isEmpty()) {
                log.debug("자동 상장폐지 진행 대상 없음");
                return;
            }
            
            int processedCount = 0;
            
            for (Stock stock : atRiskStocks) {
                try {
                    // 이 주식의 최신 위반 기록 조회
                    List<DelistingViolation> violations = violationRepo
                            .findByStockIdAndIsResolvedFalse(stock.getId());
                    
                    if (violations.isEmpty()) {
                        log.debug("위반 기록 없음: stockId={}", stock.getId());
                        continue;
                    }
                    
                    // 가장 오래된 해결되지 않은 위반 기록
                    DelistingViolation oldestViolation = violations.stream()
                            .min(Comparator.comparing(DelistingViolation::getViolationDate))
                            .orElse(null);
                    
                    if (oldestViolation == null) {
                        continue;
                    }
                    
                    // 위반 발생 후 3분 지났는지 확인
                    LocalDateTime threeMinutesAgo = LocalDateTime.now().minusMinutes(3);
                    
                    if (oldestViolation.getViolationDate().isBefore(threeMinutesAgo)) {
                        log.info("3분 유예기간 경과: stockId={}, violationDate={}, 전환 대상", 
                                stock.getId(), oldestViolation.getViolationDate());
                        
                        // 현재 stage 확인
                        DelistingStage currentStage = stock.getDelistingStage() != null ? stock.getDelistingStage() : DelistingStage.WARNING;
                        
                        // WARNING → DELISTING_NOTICE로 전환 (예고 발행)
                        if (currentStage == DelistingStage.WARNING) {
                            validateAndAutoSetDelistingStage(currentStage, DelistingStage.DELISTING_NOTICE, stock);
                            
                            stock.updateStatus(Stock.Status.DELISTING_NOTICE);
                            stock.setDelistingStage(DelistingStage.DELISTING_NOTICE);
                            stock.setDelistingNoticeDate(LocalDateTime.now());
                            stockRepo.save(stock);
                            
                            // 이력 기록
                            recordHistory(stock.getId(), ActionType.STAGE_CHANGE, 
                                         DelistingStage.WARNING, DelistingStage.DELISTING_NOTICE,
                                         "3분 유예기간 경과 - 예고 발행", 
                                         oldestViolation.getId().toString(), null);
                            
                            processedCount++;
                            log.info("자동 예고 발행 완료: stockId={}", stock.getId());
                        }
                    }
                    
                } catch (Exception e) {
                    log.error("자동 예고 발행 실패: stockId={}", stock.getId(), e);
                }
            }
            
            if (processedCount > 0) {
                log.info("자동 예고 발행 완료: {} 개 주식", processedCount);
            }
            
        } catch (Exception e) {
            log.error("자동 예고 발행 처리 중 오류 발생", e);
        }
    }

    /**
     * 계좌번호 조회 (개선된 버전)
     * 
     * 계좌 타입별로 순차 조회:
     * 1. CORPORATION (mkx-platform)
     * 2. EXCHANGE (mkx-platform)
     * 3. MEMBER (ordering-service)
     * 4. BROKERAGE (향후 지원)
     * 
     * @param accountId 계좌 UUID
     * @return 계좌번호 (못 찾으면 null)
     */
    private String findAccountNumber(UUID accountId) {
        log.debug("계좌번호 조회 시작: accountId={}", accountId);
        
        // 1. CORPORATION 계좌 조회 (mkx-platform)
        try {
            CorporationAccount corpAccount = corporationAccountRepo.findById(accountId).orElse(null);
            if (corpAccount != null) {
                String accountNumber = corpAccount.getAccountNumber();
                if (accountNumber == null || accountNumber.isEmpty()) {
                    log.error("❌ CORPORATION 계좌는 존재하지만 account_number가 없음: accountId={}", accountId);
                    return null;
                }
                log.info("✅ CORPORATION 계좌 발견: accountId={}, accountNumber={}", accountId, accountNumber);
                
                // account_list에서 타입 재확인 (데이터 무결성 검증)
                verifyAccountType(accountNumber, AccountType.CORPORATION);
                return accountNumber;
            }
        } catch (Exception e) {
            log.debug("CORPORATION 계좌 조회 중 예외: accountId={}, error={}", accountId, e.getMessage());
        }
        
        // 2. EXCHANGE 계좌 조회 (mkx-platform)
        try {
            ExchangeAccount exchangeAccount = exchangeAccountRepo.findById(accountId).orElse(null);
            if (exchangeAccount != null) {
                String accountNumber = exchangeAccount.getAccountNumber();
                if (accountNumber == null || accountNumber.isEmpty()) {
                    log.error("❌ EXCHANGE 계좌는 존재하지만 account_number가 없음: accountId={}", accountId);
                    return null;
                }
                log.info("✅ EXCHANGE 계좌 발견: accountId={}, accountNumber={}", accountId, accountNumber);
                
                // account_list에서 타입 재확인
                verifyAccountType(accountNumber, AccountType.EXCHANGE);
                return accountNumber;
            }
        } catch (Exception e) {
            log.debug("EXCHANGE 계좌 조회 중 예외: accountId={}, error={}", accountId, e.getMessage());
        }
        
        // 3. MEMBER 계좌 조회 (ordering-service)
        try {
            Map<String, Object> response = memberAccountClient.getAccountNumber(accountId);
            if (response != null) {
                // ordering 서비스는 CommonDTO 형식으로 응답: { result: { success, accountNumber } }
                Object resultObj = response.get("result");
                if (resultObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> result = (Map<String, Object>) resultObj;
                    if (Boolean.TRUE.equals(result.get("success"))) {
                        String accountNumber = (String) result.get("accountNumber");
                        if (accountNumber != null && !accountNumber.isEmpty()) {
                            log.info("✅ MEMBER 계좌 발견: accountId={}, accountNumber={}", accountId, accountNumber);
                            
                            // account_list에서 타입 재확인
                            verifyAccountType(accountNumber, AccountType.MEMBER);
                            return accountNumber;
                        } else {
                            log.error("❌ MEMBER 계좌는 존재하지만 account_number가 없음: accountId={}", accountId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("MEMBER 계좌 조회 실패: accountId={}, error={}", accountId, e.getMessage(), e);
        }
        
        // 4. 모든 타입에서 찾지 못함
        log.error("❌ 계좌번호(account_number) 조회 실패: accountId={}", accountId);
        log.error("   가능한 원인:");
        log.error("   1) 계좌가 아예 존재하지 않음");
        log.error("   2) 계좌는 있지만 account_number 컬럼이 NULL 또는 빈 문자열");
        log.error("   ⚠️ 데이터 무결성 문제 - 계좌 생성 시 account_number가 제대로 설정되지 않음");
        
        return null;
    }
    
    /**
     * account_list에서 계좌 타입 검증
     * 
     * @param accountNumber 계좌번호
     * @param expectedType 예상 타입
     */
    private void verifyAccountType(String accountNumber, AccountType expectedType) {
        try {
            AccountList accountList = accountListRepo.findByAccountNumber(accountNumber).orElse(null);
            if (accountList == null) {
                log.warn("⚠️ account_list에 등록되지 않은 계좌: accountNumber={}", accountNumber);
                return;
            }
            
            if (accountList.getType() != expectedType) {
                log.warn("⚠️ 계좌 타입 불일치: accountNumber={}, expected={}, actual={}", 
                        accountNumber, expectedType, accountList.getType());
            } else {
                log.debug("✅ 계좌 타입 검증 성공: accountNumber={}, type={}", accountNumber, expectedType);
            }
        } catch (Exception e) {
            log.warn("account_list 검증 중 예외: accountNumber={}, error={}", accountNumber, e.getMessage());
        }
    }

    /**
     * 상장폐지 환불 이벤트 발행
     * Ledger 기록을 위해 transaction-events 토픽으로 발행
     * 
     * @param accountNumber 계좌번호
     * @param accountId 계좌 ID
     * @param accountType 계좌 타입 (MEMBER, CORPORATION)
     * @param amount 환불 금액
     * @param stockId 상장폐지 주식 ID
     * @param ticker 상장폐지 주식 ticker
     * @param quantity 주식 수량
     * @param pricePerShare 주당 가격
     */
    private void publishDelistingRefundEvent(String accountNumber, String accountId, 
                                             String accountType, Long amount, 
                                             UUID stockId, String ticker,
                                             Long quantity, Long pricePerShare) {
        try {
            TransactionEvent event = TransactionEvent.builder()
                    .eventId(UUID.randomUUID().toString())
                    .accountNumber(accountNumber)
                    .accountId(accountId)
                    .accountType(accountType)
                    .transactionType("DELISTING_REFUND")  // ⭐ 상장폐지 환불 타입으로 명시
                    .amount(amount)
                    .method("SYSTEM_REFUND")
                    .description("상장폐지 환불: ticker=" + ticker + ", stockId=" + stockId)
                    .timestamp(System.currentTimeMillis())
                    // ⭐ 상장폐지 상세 정보
                    .ticker(ticker)
                    .quantity(quantity)
                    .pricePerShare(pricePerShare)
                    .build();
            
            transactionKafkaTemplate.send(TRANSACTION_TOPIC, accountNumber, event);
            log.info("✅ 상장폐지 환불 이벤트 발행 완료: accountNumber={}, ticker={}, quantity={}주, price={}원, totalAmount={}", 
                    accountNumber, ticker, quantity, pricePerShare, amount);
        } catch (Exception e) {
            log.error("❌ 상장폐지 환불 이벤트 발행 실패: accountNumber={}, stockId={}, ticker={}", 
                    accountNumber, stockId, ticker, e);
            // 이벤트 발행 실패는 환불 자체를 롤백하지 않음 (보상 트랜잭션으로 처리 필요)
        }
    }
}
