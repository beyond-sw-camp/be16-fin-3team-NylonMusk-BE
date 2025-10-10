package com.beyond.MKX.infrastructure.kafka.event;

import com.beyond.MKX.domain.order.entity.OrderEvent;
import com.beyond.MKX.domain.order.service.MatchingEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InboundOrderProcessor {

    private final MatchingEngineService matchingEngine;

    public void handleInbound(InboundOrderMessage m) {
        // place-order 스키마 → 우리 매칭 엔진 이벤트로 매핑
        OrderEvent evt = new OrderEvent();
        evt.setBrokerageId(m.getBrokerageId());
        evt.setOrderId(m.getOrderId());
        evt.setTicker(m.getTicker());
        evt.setSide(m.getSide());
        evt.setQuantity(m.getQuantity());
        evt.setPrice(m.getPrice());
        evt.setOrderType(m.getOrderKind());

        matchingEngine.process(evt);
    }
}
