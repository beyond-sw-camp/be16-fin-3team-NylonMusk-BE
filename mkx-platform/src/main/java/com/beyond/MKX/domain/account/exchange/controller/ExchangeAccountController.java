package com.beyond.MKX.domain.account.exchange.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.ExchangeOnly;
import com.beyond.MKX.common.dto.AmountRequest;
import com.beyond.MKX.domain.account.exchange.entity.ExchangeAccount;
import com.beyond.MKX.domain.account.exchange.service.ExchangeAccountService;
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
@RequestMapping("/accounts/exchange")
@RequiredArgsConstructor
public class ExchangeAccountController {

    private final ExchangeAccountService service;

    @ExchangeOnly
    @GetMapping("/{accountNumber}")
    public ResponseEntity<?> getByAccountNumber(@PathVariable String accountNumber) {
        ExchangeAccount acc = service.getByAccountNumber(accountNumber);
        return ApiResponse.ok(acc, "거래소 계좌 조회 성공");
    }

    @ExchangeOnly
    @PostMapping("/{accountNumber}/deposit")
    public ResponseEntity<?> deposit(@PathVariable String accountNumber, @RequestBody AmountRequest req) {
        BigInteger balance = service.deposit(accountNumber, req.getAmount());
        return ApiResponse.ok(Map.of("balance", balance), "입금 완료");
    }

    @ExchangeOnly
    @PostMapping("/{accountNumber}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable String accountNumber, @RequestBody AmountRequest req) {
        BigInteger balance = service.withdraw(accountNumber, req.getAmount());
        return ApiResponse.ok(Map.of("balance", balance), "출금 완료");
    }
}
