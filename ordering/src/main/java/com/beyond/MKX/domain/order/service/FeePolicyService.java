package com.beyond.MKX.domain.order.service;

import com.beyond.MKX.domain.order.dto.CommissionAndTaxData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class FeePolicyService {

    private final RedisTemplate<String, String> redisTemplate;

    public FeePolicyService(@Qualifier("feePolicyTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final String KEY_PREFIX_COMMISSION = "fee:brokerage:";
    private static final String KEY_PREFIX_TAX = "tax:transaction:rate";
    private static final Long FEE_DIVISOR = 1_000_000L; // 0.015% = 150 / 1_000_000
    private static final Long DEFAULT_COMMISSION = 150L; // 기본 수수료율 (0.015%)
    private static final Long DEFAULT_TAX = 1500L; // 기본 거래세 (0.15%) - 추후 RDB 조회로 바꿔야 됨.

    private String getKeyPrefixCommission(UUID brokerageId) {
        return KEY_PREFIX_COMMISSION + brokerageId;
    }

    /**
     * 거래금액(transactionAmount)에 대해
     * Redis에 저장된 증권사 수수료율을 적용해
     * 예수금(거래금액 + 수수료)을 계산한다.
     */
    public Long estimateBidFee(Long transactionAmount, UUID brokerageId) {
        String brokerageFeeStr = redisTemplate.opsForValue().get(getKeyPrefixCommission(brokerageId));
        long brokerageFee;
        if (brokerageFeeStr == null) {
            brokerageFee = DEFAULT_COMMISSION;
            log.warn("매수 Redis 증권사 수수료 조회 실패. 기본 수수료 적용. brokerageId: {}", brokerageId);
        } else {
            brokerageFee = Long.parseLong(brokerageFeeStr);
        }

        // 거래 금액, 수수료율, 제수를 BigDecimal로 변환
        return calculateFee(transactionAmount, brokerageFee);
    }

    /**
     * 매도 거래금액(transactionAmount)에 대해
     * Redis에 저장된 증권사 수수료율, 거래세를 적용해
     * 증권사 수수료, 매도 거래세를 계산한다.
     */
    public CommissionAndTaxData estimateAckFee(Long transactionAmount, UUID brokerageId) {
        // Redis에서 수수료와 거래세 한번에 조회.
        List<String> multiGetResult = redisTemplate.opsForValue().multiGet(
                Arrays.asList(getKeyPrefixCommission(brokerageId), KEY_PREFIX_TAX)
        );

        long commissionRate;
        if (multiGetResult.get(0) == null) {
            commissionRate = DEFAULT_COMMISSION;
            log.warn("매도 Redis 증권사 수수료 조회 실패. 기본 수수료 적용. brokerageId: {}", brokerageId);
        } else {
            commissionRate = Long.parseLong(multiGetResult.get(0));
        }

        long taxRate = multiGetResult.get(1) != null ? Long.parseLong(multiGetResult.get(1)) : DEFAULT_TAX;

        // 수수료 계산
        Long calculateCommission = calculateFee(transactionAmount, commissionRate);
        Long calculateTax = calculateFee(transactionAmount, taxRate);

        return CommissionAndTaxData.builder()
                .commission(calculateCommission)
                .tax(calculateTax)
                .build();
    }

    /// **------ 내부 메서드 ------** //

    // 수수료 및 세금 계산 메서드
    private static Long calculateFee(Long transactionAmount, long feeRate) {
        // 거래 금액, 수수료율, 제수를 BigDecimal로 변환
        BigDecimal amountBD = new BigDecimal(transactionAmount);
        BigDecimal feeBD = new BigDecimal(feeRate);
        BigDecimal divisorBD = new BigDecimal(FEE_DIVISOR);

        // 수수료율 계산 (BigDecimal.divide 사용)
        // 10자리 정밀도로 나누기 (반올림)
        BigDecimal feeRateBD = feeBD.divide(divisorBD, 10, RoundingMode.HALF_UP);

        // 수수료 금액 계산: amount * feeRate
        BigDecimal calculatedFee = amountBD.multiply(feeRateBD);

        // 최종 원 단위 처리 (1원 미만 절사)
        BigDecimal finalFee = calculatedFee.setScale(0, RoundingMode.DOWN);
        return finalFee.longValueExact();
    }

}
