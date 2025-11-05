package com.beyond.MKX.domain.order.service;

import com.beyond.MKX.domain.order.entity.OrderEvent;
import com.beyond.MKX.domain.order.repository.RedisOrderRepository;
import com.beyond.MKX.domain.order.repository.RedisOrderRepository.MatchResult;
import com.beyond.MKX.domain.order.repository.RedisOrderRepository.TradeFill;
import com.beyond.MKX.infrastructure.kafka.KafkaOrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 매칭 엔진 서비스.
 *
 * 전제
 * - 원화 정수 가격(long), 소수점 없는 정수 수량만 허용(주문·체결 모두)
 * - 총 체결 금액(totalAmount)은 원화 정수의 합계(소수 불가)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEngineService {

    private final RedisOrderRepository redisRepo;
    private final KafkaOrderProducer kafkaOrderProducer;

    /** 한 번의 Lua 실행에서 소비할 최대 매칭 수(과도한 처리 방지) */
    private static final int MAX_MATCHES_PER_CALL = 200;

    /** BigDecimal 수량이 정수인지 검증하고 long으로 변환(정수가 아니면 예외) */
    private static long toUnitsExact(BigDecimal q, String fieldName) {
        if (q == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        // 소수점 수량 방지: scale==0 인지 확인
        if (q.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException(fieldName + " must be an integer quantity (no decimals)");
        }
        return q.longValueExact();
    }

    /**
     * 주문 이벤트 엔트리 포인트.
     */
    public void process(OrderEvent event) {
        if (event == null || event.getOrderType() == null) {
            log.warn("Received null/invalid event: {}", event);
            return;
        }

        try {
            switch (event.getOrderType()) {
                case "LIMIT" -> handleLimit(event);   // 지정가 (잔량 적재)
                case "MARKET" -> handleMarket(event); // 시장가 (잔량 미적재)
                case "CANCEL" -> handleCancel(event); // 취소
                default -> log.warn("Unknown order type: {}", event.getOrderType());
            }
        } catch (Exception ex) {
            log.error("Processing failed for event: {}", event, ex);
            kafkaOrderProducer.sendError(
                    event.getOrderId() != null ? event.getOrderId() : "UNKNOWN",
                    "processing-failure:" + ex.getClass().getSimpleName(),
                    event.getAccountId()
            );
        }
    }

    /**
     * 지정가 주문 처리
     */
    private void handleLimit(OrderEvent e) {
        // --- 입력 검증 ---
        requireNonEmpty(e.getTicker(), "ticker");
        requireNonEmpty(e.getOrderId(), "orderId");
        requireNonEmpty(e.getSide(),   "side");
        requirePositive(e.getQuantity(), "quantity");
        requirePositive(e.getPrice(),    "price");

        // 정수 수량 전제 확인(요청 수량도 정수만 허용)
        final long reqUnits = toUnitsExact(e.getQuantity(), "quantity");

        final BigDecimal reqQtyBD = e.getQuantity(); // 외부 인터페이스 호환용
        final long guardPxInt = redisRepo.asIntPrice(e.getPrice()); // 지정가 → KRW 정수
        final long limitPxInt = guardPxInt;

        // 매칭 + (잔량 시) 동일 orderId/가격으로 적재
        MatchResult res = redisRepo.matchOrAddLimit(
                e.getTicker(),
                e.getSide(),
                reqQtyBD,                 // 레포 인터페이스(BigDecimal) 유지
                MAX_MATCHES_PER_CALL,
                guardPxInt,
                e.getOrderId(),
                guardPxInt
        );

        // --- 체결 집계(정수만) ---
        BigDecimal filledQtyBD = BigDecimal.ZERO; // 외부용
        long filledUnits = 0L;                    // 내부 정수 집계
        long notional    = 0L;                    // 총 체결 금액(원화 정수)
        Long lastPxInt   = null;

        for (TradeFill f : res.fills()) {
            // 정수 수량만 허용
            long units = toUnitsExact(f.quantity(), "fill.quantity");
            long px    = f.price();

            // notional += px * units (오버플로우 방지)
            long lineAmount = Math.multiplyExact(px, units);
            notional        = Math.addExact(notional, lineAmount);
            filledUnits     = Math.addExact(filledUnits, units);
            lastPxInt       = px;

            // 외부 이벤트용 BigDecimal 수량은 그대로 전달(정수이지만 타입 유지)
            filledQtyBD = filledQtyBD.add(f.quantity());

            kafkaOrderProducer.sendExecution(
                    e.getOrderId(),
                    e.getTicker(),
                    e.getSide(),
                    f.counterOrderId(),
                    f.quantity(),
                    px,
                    e.getAccountId()
            );
        }

        // 평균가(VWAP) 정수 반올림: notional / filledUnits (HALF_UP)
        Long vwapInt = (filledUnits > 0)
                ? ((notional + (filledUnits / 2)) / filledUnits)
                : null;

        BigDecimal remainingBD = res.remaining();
        // remaining(잔량)도 정수만 허용
        long remainingUnits = toUnitsExact(remainingBD, "remaining");

        if (remainingUnits <= 0L) {
            // 완전 체결
            kafkaOrderProducer.sendMarketFilled(
                    e.getOrderId(), e.getTicker(), e.getSide(),
                    vwapInt != null ? vwapInt : 0L,
                    lastPxInt != null ? lastPxInt : 0L,
                    limitPxInt,
                    filledQtyBD,
                    notional,
                    e.getAccountId()
            );
            log.info("LIMIT filled: {} {} {} filledUnits={}", e.getOrderId(), e.getTicker(), e.getSide(), reqUnits);
        } else if (res.fills().isEmpty()) {
            // 무체결 → 잔량 적재 보강
            boolean added = redisRepo.ensureLimitOrderPresent(
                    e.getTicker(), e.getOrderId(), e.getSide(), e.getPrice(), reqQtyBD);
            kafkaOrderProducer.sendNewAccepted(e.getOrderId(), e.getTicker(), e.getSide(), e.getPrice(), reqQtyBD, e.getAccountId());
            log.info("LIMIT accepted(no fills): {} {} {} qty={} price={} (fallbackAdded={})",
                    e.getOrderId(), e.getTicker(), e.getSide(), reqQtyBD, e.getPrice(), added);
        } else {
            // 부분 체결
            boolean added = redisRepo.ensureLimitOrderPresent(
                    e.getTicker(), e.getOrderId(), e.getSide(), e.getPrice(), remainingBD);

            kafkaOrderProducer.sendMarketPartial(
                    e.getOrderId(), e.getTicker(), e.getSide(),
                    remainingBD,
                    vwapInt != null ? vwapInt : 0L,
                    lastPxInt != null ? lastPxInt : 0L,
                    limitPxInt,
                    filledQtyBD,
                    notional,
                    e.getAccountId()
            );

            log.info("LIMIT partial: {} {} {} remainingUnits={} (fallbackAdded={})",
                    e.getOrderId(), e.getTicker(), e.getSide(), remainingUnits, added);
        }
    }

    /**
     * 시장가 주문 처리
     */
    private void handleMarket(OrderEvent mkt) {
        requireNonEmpty(mkt.getTicker(), "ticker");
        requireNonEmpty(mkt.getOrderId(), "orderId");
        requireNonEmpty(mkt.getSide(),   "side");
        requirePositive(mkt.getQuantity(), "quantity");
        requirePositive(mkt.getPrice(),    "guard price");

        // 시장가도 정수 수량만 허용
        final long reqUnits = toUnitsExact(mkt.getQuantity(), "quantity");

        final BigDecimal reqQtyBD = mkt.getQuantity();
        final long guardPxInt = redisRepo.asIntPrice(mkt.getPrice());
        final long limitPxInt = guardPxInt;

        // 시장가: 잔량 미적재
        MatchResult res = redisRepo.matchOrAddLimit(
                mkt.getTicker(),
                mkt.getSide(),
                reqQtyBD,
                MAX_MATCHES_PER_CALL,
                guardPxInt,
                "",
                0L
        );

        BigDecimal filledQtyBD = BigDecimal.ZERO;
        long filledUnits = 0L;
        long notional    = 0L;
        Long lastPxInt   = null;

        for (TradeFill f : res.fills()) {
            long units = toUnitsExact(f.quantity(), "fill.quantity");
            long px    = f.price();

            long lineAmount = Math.multiplyExact(px, units);
            notional        = Math.addExact(notional, lineAmount);
            filledUnits     = Math.addExact(filledUnits, units);
            lastPxInt       = px;

            filledQtyBD = filledQtyBD.add(f.quantity());

            kafkaOrderProducer.sendExecution(
                    mkt.getOrderId(),
                    mkt.getTicker(),
                    mkt.getSide(),
                    f.counterOrderId(),
                    f.quantity(),
                    px,
                    mkt.getAccountId()
            );
        }

        Long vwapInt = (filledUnits > 0)
                ? ((notional + (filledUnits / 2)) / filledUnits)
                : null;

        BigDecimal remainingBD = res.remaining();
        long remainingUnits = toUnitsExact(remainingBD, "remaining");

        if (remainingUnits <= 0L) {
            // 완전 체결
            kafkaOrderProducer.sendMarketFilled(
                    mkt.getOrderId(), mkt.getTicker(), mkt.getSide(),
                    vwapInt != null ? vwapInt : 0L,
                    lastPxInt != null ? lastPxInt : 0L,
                    limitPxInt,
                    filledQtyBD,
                    notional,
                    mkt.getAccountId()
            );
            log.info("MARKET filled: {} ticker={} side={} filledUnits={}", mkt.getOrderId(), mkt.getTicker(), mkt.getSide(), reqUnits);
        } else if (res.fills().isEmpty()) {
            kafkaOrderProducer.sendWaiting(mkt.getOrderId(), mkt.getTicker(), mkt.getSide(), mkt.getAccountId());
            log.info("MARKET waiting: {} (no opposite within guard)", mkt.getOrderId());
        } else {
            kafkaOrderProducer.sendMarketPartial(
                    mkt.getOrderId(), mkt.getTicker(), mkt.getSide(),
                    remainingBD,
                    vwapInt != null ? vwapInt : 0L,
                    lastPxInt != null ? lastPxInt : 0L,
                    limitPxInt,
                    filledQtyBD,
                    notional,
                    mkt.getAccountId()
            );
            log.info("MARKET partial: {} ticker={} side={} remainingUnits={}", mkt.getOrderId(), mkt.getTicker(), mkt.getSide(), remainingUnits);
        }
    }

    /**
     * 취소 주문 처리
     */
    private void handleCancel(OrderEvent e) {
        requireNonEmpty(e.getTicker(), "ticker");
        requireNonEmpty(e.getOrderId(), "orderId");
        requireNonEmpty(e.getSide(),   "side");

        redisRepo.cancelOrder(e.getOrderId(), e.getTicker(), e.getSide());
        kafkaOrderProducer.sendCancelSuccess(e.getOrderId(), e.getAccountId());
        log.info("CANCEL ok: {}", e.getOrderId());
    }

    // ----------------------------------------------------------------------
    // 입력 가드 유틸
    // ----------------------------------------------------------------------
    private static void requireNonEmpty(String v, String field) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
    private static void requirePositive(BigDecimal v, String field) {
        if (v == null || v.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
    }
    private static void requirePositive(long v, String field) {
        if (v <= 0L) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
    }
}
