package com.beyond.MKX.infrastructure.kafka.event;

import com.beyond.MKX.domain.order.entity.OrderEvent;
import com.beyond.MKX.domain.order.service.MatchingEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 인바운드 주문 메시지 → 매칭 엔진 도메인 이벤트 매핑기.
 *
 * 역할
 * - place-order 토픽에서 역직렬화된 {@link InboundOrderMessage}를 내부 도메인 모델({@link OrderEvent})로 변환
 * - 변환된 이벤트를 {@link MatchingEngineService#process(OrderEvent)}로 위임하여 LIMIT/MARKET/CANCEL 처리
 *
 * 주의
 * - price: InboundOrderMessage.price는 Long(정수 KRW), OrderEvent.price는 double.
 *   → 자바가 Long→long 언박싱 후 double로 승격하며, null이면 NPE가 발생하므로
 *   프로듀서/상위 계층에서 LIMIT은 필수, MARKET도 가드 가격을 반드시 채워 넣는 것을 전제로 한다.
 */
@Service
@RequiredArgsConstructor
public class InboundOrderProcessor {

    private final MatchingEngineService matchingEngine;

    /**
     * place-order 스키마를 매칭 엔진 입력 모델로 매핑 후 처리.
     * - 필드 일대일 매핑(brokerageId/orderId/ticker/side/quantity/price/orderType)
     * - 추가 전처리/검증은 MatchingEngineService에서 수행
     */
    public void handleInbound(InboundOrderMessage m) {
        // place-order 스키마 → 우리 매칭 엔진 이벤트로 매핑
        OrderEvent evt = OrderEvent.builder()
                .brokerageId(m.getBrokerageId())
                .orderId(m.getOrderId())
                .ticker(m.getTicker())
                .side(m.getSide())
                .orderType(m.getOrderKind())
                .price(m.getPrice())          // long
                .quantity(m.getQuantity())    // BigDecimal
                .accountId(m.getAccountId())  // 계좌 ID
                .build();

        // 매칭 엔진으로 위임(LIMIT/MARKET/CANCEL 분기 및 카프카 이벤트 발행)
        matchingEngine.process(evt);
    }
}
