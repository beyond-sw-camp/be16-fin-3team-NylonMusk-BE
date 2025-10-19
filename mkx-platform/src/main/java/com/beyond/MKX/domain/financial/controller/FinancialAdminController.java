package com.beyond.MKX.domain.financial.controller;

import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.domain.financial.dto.FinancialBundleReqDto;
import com.beyond.MKX.domain.financial.service.FinancialAggregateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class FinancialAdminController {

    private final FinancialAggregateService financialAggregateService;

    @PostMapping("/financials/bundle")
    @ExchangeOnly
    public ResponseEntity<Void> saveFinancialBundle(@RequestBody FinancialBundleReqDto req) {
        financialAggregateService.saveFinancialBundle(req);
        return ResponseEntity.ok().build();
    }
}
