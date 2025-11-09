package com.beyond.MKX.domain.tradingBot.client;

import com.beyond.MKX.common.apiResponse.CommonDTO;
import com.beyond.MKX.domain.tradingBot.dto.OrderRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * ordering-service의 주문 API FeignClient
 */
@FeignClient(name = "ordering-service", contextId = "orderClient", url = "${feign.client.url.ordering-service}")
public interface OrderClient {

    /**
     * 주문 전송
     * 
     * @param orderRequest 주문 요청 DTO
     * @return 주문 응답 (CommonDTO의 result 필드에 OrderResponseDTO 객체가 들어있음)
     */
    @PostMapping("/order")
    CommonDTO placeOrder(@RequestBody OrderRequestDTO orderRequest);
}

