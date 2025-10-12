package com.beyond.MKX.domain.account.brokerage.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.BrokerageOnly;
import com.beyond.MKX.common.auth.security.CorporationOnly;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.common.dto.AmountRequest;
import com.beyond.MKX.domain.account.brokerage.entity.BrokerageDepositAccount;
import com.beyond.MKX.domain.account.brokerage.service.BrokerageDepositAccountService;
import lombok.RequiredArgsConstructor;
import java.math.BigInteger;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounts/brokerage")
@RequiredArgsConstructor
public class BrokerageAccountController {

    private final BrokerageDepositAccountService service;

    @BrokerageOnly
    @GetMapping("/{accountNumber}")
    public ResponseEntity<?> getByAccountNumber(@PathVariable String accountNumber) {
        BrokerageDepositAccount acc = service.getByAccountNumber(accountNumber);
        return ApiResponse.ok(acc, "증권사 예치금 계좌 조회 성공");
    }

    @BrokerageOnly
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<?> deposit(@PathVariable String accountNumber, @RequestBody AmountRequest req) {
        BigInteger balance = service.deposit(accountNumber, req.getAmount());
        return ApiResponse.ok(Map.of("deposit", balance), "입금 완료");
    }

    @BrokerageOnly
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String accountNumber, @RequestBody AmountRequest req) {
        BigInteger balance = service.withdraw(accountNumber, req.getAmount());
        return ApiResponse.ok(Map.of("deposit", balance), "출금 완료");
    }
}
