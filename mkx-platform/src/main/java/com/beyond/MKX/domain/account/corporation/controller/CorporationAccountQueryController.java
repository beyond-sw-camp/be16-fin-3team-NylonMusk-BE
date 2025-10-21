package com.beyond.MKX.domain.account.corporation.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.account.corporation.dto.AccountIdResDTO;
import com.beyond.MKX.domain.account.corporation.service.CorporationAccountQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/my-stocks")
public class CorporationAccountQueryController {
    private final CorporationAccountQueryService service;

    @GetMapping("/{corpId}/account-brief")
    public AccountIdResDTO getAccountId(@PathVariable UUID corpId) {
        return service.getAccountIdByCorpId(corpId);
    }
}
