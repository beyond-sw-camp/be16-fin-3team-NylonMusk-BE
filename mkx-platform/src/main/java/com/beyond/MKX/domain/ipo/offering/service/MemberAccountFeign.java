package com.beyond.MKX.domain.ipo.offering.service;

import com.beyond.MKX.common.dto.AmountRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ordering-service", contextId = "memberAccountFeign")
public interface MemberAccountFeign {
    @PostMapping("/api/internal/member-accounts/by-number/{accountNumber}/withdraw")
    void withdraw(@PathVariable String accountNumber,
                  @RequestBody AmountRequest request);

    @PostMapping("/api/internal/member-accounts/by-number/{accountNumber}/deposit")
    void deposit(@PathVariable String accountNumber,
                 @RequestBody AmountRequest request);
}
