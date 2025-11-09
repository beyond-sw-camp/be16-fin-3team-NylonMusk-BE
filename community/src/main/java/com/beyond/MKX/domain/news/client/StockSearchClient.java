package com.beyond.MKX.domain.news.client;

import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "mkx-platform-service", contextId = "stockSearchClient", url = "http://mkx-platform-service")
public interface StockSearchClient {

    @GetMapping("/api/stocks")
    PageResponse<StockItem> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    );

    @Getter @Setter
    class PageResponse<T> {
        private List<T> content;
        private int number;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }

    @Getter @Setter
    class StockItem {
        private UUID id;
        private String ticker;
        private String nameKo;
    }
}
