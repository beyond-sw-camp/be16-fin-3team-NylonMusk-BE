package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.domain.assets.dto.StockInfoResDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "mkx-platform-service", contextId = "ledgerStockFeign")
public interface LedgerStockFeign {
    @GetMapping("/api/internal/stocks/{ticker}")
    StockInfoResDTO getStockByTicker(@PathVariable String ticker);
}

