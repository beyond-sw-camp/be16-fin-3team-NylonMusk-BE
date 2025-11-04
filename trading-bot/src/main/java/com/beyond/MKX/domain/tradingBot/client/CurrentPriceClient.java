package com.beyond.MKX.domain.tradingBot.client;

import com.beyond.MKX.common.apiResponse.CommonDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * market-data-service의 현재가 조회 FeignClient
 */
@FeignClient(name = "market-data-service", contextId = "currentPriceClient")
public interface CurrentPriceClient {

    /**
     * 특정 ticker의 현재가 조회
     * 
     * @param ticker 주식 티커
     * @return 현재가 정보 (CommonDTO의 result 필드에 CurrentPrice 객체가 들어있음)
     */
    @GetMapping("/api/v1/market/price/{ticker}")
    CommonDTO getCurrentPrice(@PathVariable("ticker") String ticker);
}

