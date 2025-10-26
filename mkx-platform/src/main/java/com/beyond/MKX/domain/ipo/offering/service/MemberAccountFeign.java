package com.beyond.MKX.domain.ipo.offering.service;

import com.beyond.MKX.common.dto.AmountRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigInteger;
import java.util.UUID;

@FeignClient(name = "ordering-service", contextId = "memberAccountInternalClient")
public interface MemberAccountFeign {
    @PostMapping("/api/internal/member-accounts/{accountNumber}/withdraw")
    void withdraw(@PathVariable String accountNumber,
                  @RequestParam AmountRequest request);

    @PostMapping("/api/internal/member-accounts/{accountNumber}/deposit")
    void deposit(@PathVariable String accountNumber,
                 @RequestParam AmountRequest request);
}
