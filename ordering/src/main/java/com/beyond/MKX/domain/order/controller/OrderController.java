package com.beyond.MKX.domain.order.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.order.dto.OrderCancelRequestDTO;
import com.beyond.MKX.domain.order.dto.OrderRequestDTO;
import com.beyond.MKX.domain.order.dto.OrderResponseDTO;
import com.beyond.MKX.domain.order.service.OrderBookService;
import com.beyond.MKX.domain.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderBookService orderBookService;

    @PostMapping
    public ResponseEntity<?> placeOrder(
            @RequestBody @Valid OrderRequestDTO reqDTO
    ) {
        OrderResponseDTO resDTO = orderService.placeOrder(reqDTO);

        return ApiResponse.created(resDTO);
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelOrder(
            @RequestBody @Valid OrderCancelRequestDTO reqDTO,
            @RequestHeader("X-User-Id") UUID memberId
//            @RequestHeader(value = "X-User-Role", required = false) String roleHeader
    ) {
        OrderResponseDTO resDTO = orderService.cancelOrder(reqDTO, memberId);
        return ApiResponse.ok(resDTO);
    }


    /**
     * test 목적 '최저 매도가' 조회 매핑
     * @return ResponseEntity
     */
    @GetMapping("/test/best-ask/{ticker}")
    public ResponseEntity<?> getBestAskByRedisOrderBook(
            @PathVariable String ticker
    ) {
        Optional<Long> lowestAsk = orderBookService.getLowestAsk(ticker);
        return ApiResponse.ok(lowestAsk.orElse(null));
    }

    /**
     * test 목적 '최고 매수가' 조회 매핑
     * @return ResponseEntity
     */
    @GetMapping("/test/best-bid/{ticker}")
    public ResponseEntity<?> getBestBidByRedisOrderBook(
            @PathVariable String ticker
    ) {
        Optional<Long> highestBid = orderBookService.getHighestBid(ticker);
        if (highestBid.isPresent()) {
            return ApiResponse.ok(highestBid.get());
        } else {
            return ApiResponse.ok(null, "값이 없습니다.");
        }
    }
}
