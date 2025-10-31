package com.beyond.MKX.domain.ipo.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * community 서비스의 뉴스 재매핑 API를 호출하는 FeignClient
 */
@FeignClient(name = "community-service", contextId = "newsRemappingClient")
public interface NewsRemappingClient {

    @PostMapping("/api/internal/news-articles/remap")
    Map<String, Object> remapNewsForListedStock(
            @RequestParam("stockId") String stockId,
            @RequestParam("stockNameKo") String stockNameKo,
            @RequestParam("ticker") String ticker
    );
}
