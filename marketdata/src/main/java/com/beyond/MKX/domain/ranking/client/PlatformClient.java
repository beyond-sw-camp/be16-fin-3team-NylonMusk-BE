package com.beyond.MKX.domain.ranking.client;

import com.beyond.MKX.domain.ranking.dto.CardSectionDataDTO;
import com.beyond.MKX.domain.ranking.dto.StockBriefDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    // 카드섹션 데이터 조회 (인기 + 신규 종목)
    @GetMapping("/api/internal/stocks/card-section-data")
    CardSectionDataDTO getCardSectionData(@RequestParam(defaultValue = "3") int limit);

    // 사용자의 즐겨찾기 종목 ticker 리스트 조회
    @GetMapping("/api/internal/stocks/favorites/{memberId}")
    List<String> getFavoriteTickers(@PathVariable("memberId") UUID memberId);

}
