package com.beyond.MKX.domain.order.client;

import com.beyond.MKX.domain.order.entity.OrderEvent;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "matching-engine-service")
public interface MatchingEngineClient {
    
    /**
     * 매칭엔진에 주문 전송
     * @param event 주문 이벤트
     * @return 처리 결과
     */
    @PostMapping("/test/order")
    ResponseEntity<String> sendOrder(@RequestBody OrderEvent event);
}
