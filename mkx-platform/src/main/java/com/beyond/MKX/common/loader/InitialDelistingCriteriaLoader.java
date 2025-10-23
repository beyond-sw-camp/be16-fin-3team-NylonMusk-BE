package com.beyond.MKX.common.loader;

import com.beyond.MKX.domain.delisting.entity.DelistingCriteria;
import com.beyond.MKX.domain.delisting.entity.CriteriaType;
import com.beyond.MKX.domain.delisting.repository.DelistingCriteriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 상장폐지 기준 초기화 로더
 * 
 * 애플리케이션 시작 시 기본 상장폐지 기준들을 자동으로 생성합니다.
 * 위키백과 상장폐지 요건을 기반으로 한 실제 기준값들을 설정합니다.
 * 
 * @author MKX Platform Team
 * @since 2025-01-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialDelistingCriteriaLoader implements CommandLineRunner {

    private final DelistingCriteriaRepository criteriaRepo;

    @Override
    public void run(String... args) {
        log.info("상장폐지 기준 초기화 시작");

        // 이미 기준이 하나라도 있으면 생성 스킵 (멱등)
        if (criteriaRepo.count() > 0) {
            log.info("상장폐지 기준이 이미 존재합니다 (count={}). 초기화를 건너뜁니다.", criteriaRepo.count());
            return;
        }

        try {
            // 재무 기준 생성
            createFinancialCriteria();
            
            // 거래 기준 생성
            createTradingCriteria();
            
            // 법규 기준 생성
            createRegulatoryCriteria();

            log.info("상장폐지 기준 초기화 완료: 총 {} 개 기준 생성", criteriaRepo.count());

        } catch (Exception e) {
            log.error("상장폐지 기준 초기화 중 오류 발생", e);
        }
    }

    /**
     * 재무 기준 생성
     */
    private void createFinancialCriteria() {
        log.info("재무 기준 생성 시작");

        // 1. 매출액 기준 (2년간 평균 50억원 미만)
        createCriteria(
            "LOW_REVENUE_2Y",
            "2년간 저매출",
            CriteriaType.FINANCIAL,
            new BigDecimal("5000000000"), // 50억원
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            2,
            DelistingCriteria.ThresholdUnit.YEARS,
            "최근 2년간 평균 매출액이 50억원 미만인 경우"
        );

        // 2. 순이익 마이너스
        createCriteria(
            "NEGATIVE_NET_INCOME",
            "순이익 마이너스",
            CriteriaType.FINANCIAL,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "순이익이 마이너스인 경우"
        );

        // 3. 자기자본비율 기준 (20% 미만)
        createCriteria(
            "LOW_EQUITY_RATIO",
            "저자기자본비율",
            CriteriaType.FINANCIAL,
            new BigDecimal("20.0"), // 20%
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "자기자본비율이 20% 미만인 경우"
        );

        // 4. 부채비율 기준 (200% 초과)
        createCriteria(
            "HIGH_DEBT_RATIO",
            "고부채비율",
            CriteriaType.FINANCIAL,
            new BigDecimal("200.0"), // 200%
            DelistingCriteria.ComparisonOperator.GREATER_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "부채비율이 200% 초과인 경우"
        );

        // 5. 유동비율 기준 (1.0 미만)
        createCriteria(
            "LOW_CURRENT_RATIO",
            "저유동비율",
            CriteriaType.FINANCIAL,
            new BigDecimal("1.0"),
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "유동비율이 1.0 미만인 경우"
        );

        // 6. 자본금 기준 (10억원 미만)
        createCriteria(
            "INSUFFICIENT_CAPITAL",
            "자본금 부족",
            CriteriaType.FINANCIAL,
            new BigDecimal("1000000000"), // 10억원
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.YEARS,
            "자본금이 10억원 미만인 경우"
        );

        // 7. ROE 기준 (5% 미만)
        createCriteria(
            "LOW_ROE",
            "저ROE",
            CriteriaType.FINANCIAL,
            new BigDecimal("5.0"), // 5%
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "자기자본이익률(ROE)이 5% 미만인 경우"
        );

        // 8. ROA 기준 (3% 미만)
        createCriteria(
            "LOW_ROA",
            "저ROA",
            CriteriaType.FINANCIAL,
            new BigDecimal("3.0"), // 3%
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "총자산이익률(ROA)이 3% 미만인 경우"
        );

        // 9. 영업이익 마이너스
        createCriteria(
            "NEGATIVE_OPERATING_INCOME",
            "영업이익 마이너스",
            CriteriaType.FINANCIAL,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "영업이익이 마이너스인 경우"
        );

        // 10. 이자보상배수 기준 (1.0 미만)
        createCriteria(
            "LOW_INTEREST_COVERAGE",
            "저이자보상배수",
            CriteriaType.FINANCIAL,
            new BigDecimal("1.0"),
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "이자보상배수가 1.0 미만인 경우"
        );

        log.info("재무 기준 생성 완료: 10개");
    }

    /**
     * 거래 기준 생성
     */
    private void createTradingCriteria() {
        log.info("거래 기준 생성 시작");

        // 1. 거래량 기준 (유동주식 수의 1% 미만 2분기 연속)
        createCriteria(
            "LOW_TRADING_VOLUME_2Q",
            "저거래량",
            CriteriaType.TRADING,
            new BigDecimal("1.0"), // 1%
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            2,
            DelistingCriteria.ThresholdUnit.QUARTERS,
            "분기 월 평균 거래량이 유동주식 수의 1% 미만 2분기 연속인 경우"
        );

        // 2. 주가 기준 (30일 평균 1000원 미만)
        createCriteria(
            "LOW_STOCK_PRICE_30D",
            "저주가",
            CriteriaType.TRADING,
            new BigDecimal("1000"), // 1000원
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            30,
            DelistingCriteria.ThresholdUnit.DAYS,
            "30일 평균 주가가 1000원 미만인 경우"
        );

        // 3. 시가총액 기준 (관리종목 지정 후 지속)
        createCriteria(
            "LOW_MARKET_CAP_CONTINUOUS",
            "저시가총액 지속",
            CriteriaType.TRADING,
            new BigDecimal("10000000000"), // 100억원
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            1,
            DelistingCriteria.ThresholdUnit.MONTHS,
            "관리종목 지정 후 시가총액 부족 상태가 지속되는 경우"
        );

        // 4. 소액주주 기준 (200인 미만 2년 연속)
        createCriteria(
            "LOW_MINORITY_SHAREHOLDERS",
            "저소액주주",
            CriteriaType.TRADING,
            new BigDecimal("200"), // 200인
            DelistingCriteria.ComparisonOperator.LESS_THAN,
            2,
            DelistingCriteria.ThresholdUnit.YEARS,
            "소액주주 수가 200인 미만(또는 지분 20% 미만) 2년 연속인 경우"
        );

        log.info("거래 기준 생성 완료: 4개");
    }

    /**
     * 법규 기준 생성
     */
    private void createRegulatoryCriteria() {
        log.info("법규 기준 생성 시작");

        // 1. 감사 의견 부적정
        createCriteria(
            "AUDIT_OPINION_ADVERSE",
            "감사 의견 부적정",
            CriteriaType.REGULATORY,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.EQUAL,
            1,
            DelistingCriteria.ThresholdUnit.YEARS,
            "감사 의견이 부적정인 경우"
        );

        // 2. 감사 의견 한정
        createCriteria(
            "AUDIT_OPINION_QUALIFIED",
            "감사 의견 한정",
            CriteriaType.REGULATORY,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.EQUAL,
            1,
            DelistingCriteria.ThresholdUnit.YEARS,
            "감사 의견이 한정인 경우"
        );

        // 3. 감사 의견 거절
        createCriteria(
            "AUDIT_OPINION_DISCLAIMER",
            "감사 의견 거절",
            CriteriaType.REGULATORY,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.EQUAL,
            1,
            DelistingCriteria.ThresholdUnit.YEARS,
            "감사 의견이 거절인 경우"
        );

        // 4. 보고서 제출 지연 (10일 초과)
        createCriteria(
            "REPORT_DELAY",
            "보고서 제출 지연",
            CriteriaType.REGULATORY,
            new BigDecimal("10"), // 10일
            DelistingCriteria.ComparisonOperator.GREATER_THAN,
            1,
            DelistingCriteria.ThresholdUnit.DAYS,
            "분기별 사업보고서 제출 기한 후 10일 내 미제출인 경우"
        );

        // 5. 최종 부도
        createCriteria(
            "FINAL_BANKRUPTCY",
            "최종 부도",
            CriteriaType.REGULATORY,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.EQUAL,
            1,
            DelistingCriteria.ThresholdUnit.DAYS,
            "최종 부도가 발생한 경우"
        );

        // 6. 횡령 발생
        createCriteria(
            "EMBEZZLEMENT",
            "횡령 발생",
            CriteriaType.REGULATORY,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.EQUAL,
            1,
            DelistingCriteria.ThresholdUnit.DAYS,
            "횡령이 발생한 경우"
        );

        // 7. 배임 발생
        createCriteria(
            "BREACH_OF_TRUST",
            "배임 발생",
            CriteriaType.REGULATORY,
            BigDecimal.ZERO,
            DelistingCriteria.ComparisonOperator.EQUAL,
            1,
            DelistingCriteria.ThresholdUnit.DAYS,
            "배임이 발생한 경우"
        );

        log.info("법규 기준 생성 완료: 7개");
    }

    /**
     * 개별 기준 생성 헬퍼 메서드
     */
    private void createCriteria(String criteriaCode, String criteriaName, CriteriaType criteriaType,
                               BigDecimal thresholdValue, DelistingCriteria.ComparisonOperator comparisonOperator,
                               Integer thresholdPeriod, DelistingCriteria.ThresholdUnit thresholdUnit,
                               String description) {
        
        DelistingCriteria criteria = DelistingCriteria.builder()
                .criteriaCode(criteriaCode)
                .criteriaName(criteriaName)
                .criteriaType(criteriaType)
                .thresholdValue(thresholdValue)
                .comparisonOperator(comparisonOperator)
                .thresholdPeriod(thresholdPeriod)
                .thresholdUnit(thresholdUnit)
                .description(description)
                .isActive(true)
                .createdBy(UUID.fromString("00000000-0000-0000-0000-000000000000")) // 시스템 생성
                .effectiveFrom(LocalDateTime.now())
                .effectiveTo(LocalDateTime.now().plusYears(10)) // 10년 후까지 유효
                .build();

        criteriaRepo.save(criteria);
        log.debug("기준 생성 완료: {} - {}", criteriaCode, criteriaName);
    }
}
