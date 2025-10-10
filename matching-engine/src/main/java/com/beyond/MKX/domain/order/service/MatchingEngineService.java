package com.beyond.MKX.domain.order.service;

import com.beyond.MKX.domain.order.entity.OrderEvent;
import com.beyond.MKX.domain.order.repository.RedisOrderRepository;
import com.beyond.MKX.domain.order.repository.RedisOrderRepository.MatchResult;
import com.beyond.MKX.domain.order.repository.RedisOrderRepository.TradeFill;
import com.beyond.MKX.infrastructure.kafka.KafkaOrderProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 매칭 엔진 서비스.
 *
 * 역할
 * - 주문 이벤트(OrderEvent)를 유형별로 처리(LIMIT / MARKET / CANCEL)
 * - Redis 오더북 리포지토리와 Lua 스크립트를 통해 매칭 수행
 * - 체결(Execution) 및 상태(OrderStatus) 카프카 이벤트 발행
 *
 * 주의
 * - 숫자 비교 시 double 사용: Redis/Lua 반환값과 일관 유지
 * - 예외 발생 시 order-errors 토픽으로 원인 전달
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingEngineService {

    private final RedisOrderRepository redisRepo;
    private final KafkaOrderProducer kafkaOrderProducer;

    /** 한 번의 Lua 실행에서 소비할 최대 매칭 수(과도한 처리 방지) */
    private static final int MAX_MATCHES_PER_CALL = 200;

    /**
     * 주문 이벤트 엔트리 포인트.
     * - 이벤트 null/유형 누락 시 경고 로그 후 무시
     * - 유형별 핸들러 위임
     */
    public void process(OrderEvent event) {
        if (event == null || event.getOrderType() == null) {
            log.warn("Received null/invalid event: {}", event);
            return;
        }

        try {
            switch (event.getOrderType()) {
                case "LIMIT" -> handleLimit(event);   // 지정가: 매칭 시도 후 잔량은 오더북 적재
                case "MARKET" -> handleMarket(event); // 시장가: 매칭만(잔량 미적재), 가격가드 사용
                case "CANCEL" -> handleCancel(event); // 취소
                default -> log.warn("Unknown order type: {}", event.getOrderType());
            }
        } catch (Exception ex) {
            // 예외는 에러 토픽으로 전달하여 상위(브로커리지/게이트웨이)에서 알림 가능
            log.error("Processing failed for event: {}", event, ex);
            kafkaOrderProducer.sendError(
                    event.getOrderId() != null ? event.getOrderId() : "UNKNOWN",
                    "processing-failure:" + ex.getClass().getSimpleName()
            );
        }
    }

    /**
     * 지정가 주문 처리:
     * 1) 가격 가드 = 지정가로 매칭 시도
     * 2) 잔량이 있으면 동일 orderId/가격으로 즉시 오더북 적재(Repo/Lua가 처리)
     * 3) 체결 건마다 executions 발행, 요약 상태를 order-status로 발행
     */
    private void handleLimit(OrderEvent e) {
        // --- 입력 검증(필수값·양수) ---
        requireNonEmpty(e.getTicker(), "ticker");
        requireNonEmpty(e.getOrderId(), "orderId");
        requireNonEmpty(e.getSide(), "side");
        requirePositive(e.getQuantity(), "quantity");
        requirePositive(e.getPrice(), "price");

        final long guardPxInt = redisRepo.asIntPrice(e.getPrice());

        // 매칭 + (잔량 시) 동일 orderId/가격으로 적재
        MatchResult res = redisRepo.matchOrAddLimit(
                e.getTicker(),
                e.getSide(),
                e.getQuantity(),
                MAX_MATCHES_PER_CALL,
                guardPxInt,            // 가격가드 = 지정가
                e.getOrderId(),        // 잔량 적재 시 사용할 orderId
                guardPxInt             // 잔량 적재 가격 = 지정가
        );

        // --- 체결 통계 집계(VWAP/last) ---
        double filledQty = 0.0;
        double notional  = 0.0;
        Double lastPx    = null;

        for (TradeFill f : res.fills()) {
            filledQty += f.quantity();
            notional  += f.quantity() * f.price();
            lastPx     = f.price();

            // 개별 체결 이벤트 발행(상대 주문 ID 포함)
            kafkaOrderProducer.sendExecution(
                    e.getOrderId(),
                    e.getTicker(),
                    e.getSide(),
                    f.counterOrderId(),
                    f.quantity(),
                    f.price()
            );
        }

        Double vwap    = filledQty > 0 ? (notional / filledQty) : null;
        double limitPx = e.getPrice();

        // --- 상태 이벤트(완전/부분/무체결) ---
        if (res.remaining() <= 0) {
            // 완전 체결
            kafkaOrderProducer.sendMarketFilled(
                    e.getOrderId(), e.getTicker(), e.getSide(),
                    vwap != null ? vwap : 0.0,
                    lastPx != null ? lastPx : 0.0,
                    limitPx,
                    filledQty
            );
            log.info("LIMIT filled: {} {} {} filledQty={}", e.getOrderId(), e.getTicker(), e.getSide(), e.getQuantity());
        } else if (res.fills().isEmpty()) {
            // 전혀 체결 아님 → 잔량 적재 보강(동일 키가 없을 때만)
            boolean added = redisRepo.ensureLimitOrderPresent(
                    e.getTicker(), e.getOrderId(), e.getSide(), e.getPrice(), e.getQuantity());
            kafkaOrderProducer.sendNewAccepted(e.getOrderId(), e.getTicker(), e.getSide(), e.getPrice(), e.getQuantity());
            log.info("LIMIT accepted(no fills): {} {} {} qty={} price={} (fallbackAdded={})",
                    e.getOrderId(), e.getTicker(), e.getSide(), e.getQuantity(), e.getPrice(), added);
        } else {
            // 부분 체결 + 잔량 적재 보강
            boolean added = redisRepo.ensureLimitOrderPresent(
                    e.getTicker(), e.getOrderId(), e.getSide(), e.getPrice(), res.remaining());

            kafkaOrderProducer.sendMarketPartial(
                    e.getOrderId(), e.getTicker(), e.getSide(),
                    res.remaining(),
                    vwap != null ? vwap : 0.0,
                    lastPx != null ? lastPx : 0.0,
                    limitPx,
                    filledQty
            );

            log.info("LIMIT partial: {} {} {} remaining={} (fallbackAdded={})",
                    e.getOrderId(), e.getTicker(), e.getSide(), res.remaining(), added);
        }
    }

    /**
     * 시장가 주문 처리:
     * 1) 가격 가드 = 이벤트 price로 매칭만 수행
     * 2) 잔량은 오더북에 적재하지 않음
     * 3) 체결 건마다 executions 발행, 요약 상태를 order-status로 발행
     */
    private void handleMarket(OrderEvent mkt) {
        requireNonEmpty(mkt.getTicker(), "ticker");
        requireNonEmpty(mkt.getOrderId(), "orderId");
        requireNonEmpty(mkt.getSide(), "side");
        requirePositive(mkt.getQuantity(), "quantity");
        requirePositive(mkt.getPrice(), "guard price");

        final long guardPxInt = redisRepo.asIntPrice(mkt.getPrice());

        // 시장가: orderIdToAdd="", priceIntForAdd=0 전달(잔량 미적재)
        MatchResult res = redisRepo.matchOrAddLimit(
                mkt.getTicker(),
                mkt.getSide(),
                mkt.getQuantity(),
                MAX_MATCHES_PER_CALL,
                guardPxInt,
                "",     // 시장가: 잔량 적재 안 함
                0L
        );

        // --- 체결 통계 집계(VWAP/last) ---
        double filledQty = 0.0;
        double notional  = 0.0;
        Double lastPx    = null;

        for (TradeFill f : res.fills()) {
            filledQty += f.quantity();
            notional  += f.quantity() * f.price();
            lastPx     = f.price();

            kafkaOrderProducer.sendExecution(
                    mkt.getOrderId(),
                    mkt.getTicker(),
                    mkt.getSide(),
                    f.counterOrderId(),
                    f.quantity(),
                    f.price()
            );
        }

        Double vwap = filledQty > 0 ? (notional / filledQty) : null;
        // MARKET의 limitPrice 필드는 '가드 가격' 의미로 재사용(primitive double null 회피)
        double limitPx = mkt.getPrice();

        if (res.remaining() <= 0.0) {
            // 완전 체결
            kafkaOrderProducer.sendMarketFilled(
                    mkt.getOrderId(), mkt.getTicker(), mkt.getSide(),
                    vwap != null ? vwap : 0.0,
                    lastPx != null ? lastPx : 0.0,
                    limitPx,
                    filledQty
            );
            log.info("MARKET filled: {} ticker={} side={} filledQty={}", mkt.getOrderId(), mkt.getTicker(), mkt.getSide(), mkt.getQuantity());
        } else if (res.fills().isEmpty()) {
            // 가드 범위 내 반대 호가 없음 → 대기(체결 X)
            kafkaOrderProducer.sendWaiting(mkt.getOrderId(), mkt.getTicker(), mkt.getSide());
            log.info("MARKET waiting: {} (no opposite within guard)", mkt.getOrderId());
        } else {
            // 부분 체결
            kafkaOrderProducer.sendMarketPartial(
                    mkt.getOrderId(), mkt.getTicker(), mkt.getSide(),
                    res.remaining(),
                    vwap != null ? vwap : 0.0,
                    lastPx != null ? lastPx : 0.0,
                    limitPx,
                    filledQty
            );
            log.info("MARKET partial: {} ticker={} side={} remaining={}", mkt.getOrderId(), mkt.getTicker(), mkt.getSide(), res.remaining());
        }
    }

    /**
     * 취소 주문 처리:
     * - 오더북 ZSET/주문 상세/인덱스를 제거
     * - CANCEL_OK 상태 이벤트 발행
     */
    private void handleCancel(OrderEvent e) {
        requireNonEmpty(e.getTicker(), "ticker");
        requireNonEmpty(e.getOrderId(), "orderId");
        requireNonEmpty(e.getSide(), "side");

        redisRepo.cancelOrder(e.getOrderId(), e.getTicker(), e.getSide());
        kafkaOrderProducer.sendCancelSuccess(e.getOrderId());
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
    private static void requirePositive(double v, String field) {
        if (v <= 0.0) {
            throw new IllegalArgumentException(field + " must be > 0");
        }
    }
}
