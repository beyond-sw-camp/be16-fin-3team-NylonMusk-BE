package com.beyond.MKX.domain.trade.client;

import com.beyond.MKX.domain.trade.dto.StockBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "mkx-platform-service",
//        url = "${platform.service.url}",           // ex) http://mkx-platform-service:8080
        contextId = "stockInternalClient"
)
public interface PlatformClient {


    // 종목 이름 조회 - 리스트
    @GetMapping("/api/internal/stocks/names")
    Map<String, String> getNames(@RequestParam("tickers") List<String> tickers);

    // 종목 정보 리스트 조회
    @GetMapping("/api/internal/stocks/briefs")
    List<StockBriefDTO> getBriefs(@RequestParam("tickers") List<String> tickers);

}
