package com.beyond.MKX.domain.trade.service;

import com.beyond.MKX.domain.trade.client.PlatformClient;
import com.beyond.MKX.domain.trade.dto.StockBriefDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@Service
@RequiredArgsConstructor
public class StockMetaService {

    private final PlatformClient platformClient;

    /**
     * 캐시 사용 안 함. 매 호출마다 플랫폼으로 직접 질의.
     */
    public List<StockBriefDTO> fetchBriefs(List<String> tickers) {
        if (tickers == null || tickers.isEmpty()) return List.of();
        return platformClient.getBriefs(tickers);
    }

    /**
     * ticker -> brief 매핑이 필요할 때 편의 메서드
     */
    public Map<String, StockBriefDTO> fetchBriefMap(List<String> tickers) {
        return fetchBriefs(tickers).stream()
                .filter(b -> b.getTicker() != null)
                .collect(Collectors.toMap(
                        StockBriefDTO::getTicker,
                        Function.identity(),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
    }


}
