package com.beyond.MKX.domain.delisting.client;

import com.beyond.MKX.domain.delisting.dto.CurrentPriceResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * marketdata 서비스의 현재가 정보를 조회하는 FeignClient
 * 상장폐지 보상금 계산 시 현재 주가가 필요함
 */
@FeignClient(name = "marketdata-service", contextId = "currentPriceClient")
public interface CurrentPriceClient {

    /**
     * 특정 ticker의 현재가 조회
     * 
     * @param ticker 주식 티커
     * @return 현재가 정보
     */
    @GetMapping("/api/v1/market/price/{ticker}")
    CurrentPriceResDto getCurrentPrice(@PathVariable("ticker") String ticker);
}
