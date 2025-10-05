package com.beyond.MKX.domain.order.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Component
public class FeePolicyService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX_COMMISSION = "fee:brokerage:";
    private static final Long FEE_DIVISOR = 1_000_000L; // 0.015% = 150 / 1_000_000
    private static final Long DEFAULT_FEE = 150L; // 기본 수수료율 (0.015%)

    private String key(UUID brokerageId) {
        return KEY_PREFIX_COMMISSION + brokerageId;
    }

    public FeePolicyService(@Qualifier("feePolicyTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }


    /**
     * 거래금액(transactionAmount)에 대해
     * Redis에 저장된 증권사 수수료율을 적용해
     * 예수금(거래금액 + 수수료)을 계산한다.
     */
    public Long estimateAckFee(Long transactionAmount, UUID brokerageId) {
        String brokerageFeeStr = redisTemplate.opsForValue().get(key(brokerageId));

        long brokerageFee = brokerageFeeStr != null ? Long.parseLong(brokerageFeeStr) : DEFAULT_FEE;

        // 거래 금액, 수수료율, 제수를 BigDecimal로 변환
        BigDecimal amountBD = new BigDecimal(transactionAmount);
        BigDecimal feeBD = new BigDecimal(brokerageFee);
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


//    STT
}
