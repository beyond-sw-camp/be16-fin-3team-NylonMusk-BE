package com.beyond.MKX.common.test.controller;

import com.beyond.MKX.domain.order.entity.OrderEvent;
import com.beyond.MKX.domain.order.service.MatchingEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestController {

    private final MatchingEngineService matchingEngineService;

    @PostMapping("/order")
    public ResponseEntity<String> sendOrder(@RequestBody OrderEvent event) {
        matchingEngineService.process(event);
        return ResponseEntity.ok("Order processed: " + event.getOrderId());
    }
}
