package com.beyond.MKX.domain.financial.controller;

import com.beyond.MKX.domain.financial.dto.FinancialBundleResDto;
import com.beyond.MKX.domain.financial.service.FinancialAggregateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public")
public class FinancialPublicController {

    private final FinancialAggregateService financialAggregateService;

    @GetMapping("/{ticker}/financials/")
    public ResponseEntity<FinancialBundleResDto> getFinancialsBundle(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "quarterly") String range // quarterly|annual
    ) {
        return ResponseEntity.ok(financialAggregateService.getBundleByTicker(ticker, range));
    }
}
