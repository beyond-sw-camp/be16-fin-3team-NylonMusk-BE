package com.beyond.MKX.common.loader;

import com.beyond.MKX.domain.account.exchange.repository.ExchangeAccountRepository;
import com.beyond.MKX.domain.account.exchange.service.ExchangeAccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 초기 거래소 운영 계좌를 멱등하게 생성한다.
 * - account_list에도 함께 등록된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitialExchangeAccountLoader implements CommandLineRunner {

    private final ExchangeAccountRepository exchangeAccountRepository;
    private final ExchangeAccountService exchangeAccountService;

    @Value("${init.exchange.accountNumber:}")
    private String exchangeAccountNumber;

    @Override
    public void run(String... args) {
        // 이미 하나라도 있으면 생성 스킵 (멱등)
        if (exchangeAccountRepository.count() > 0) {
            log.info("Exchange account already exists (count>0). Skip seeding.");
            return;
        }

        // 번호 결정: 미설정이면 기본 포맷 사용
        String desired = exchangeAccountNumber;
        if (desired == null || desired.isBlank()) {
            desired = com.beyond.MKX.domain.account.accountlist.util.AccountNumberGenerator.exchangeDefault();
            log.info("Exchange account number not configured. Using default: {}", desired);
        }

        // 동일 번호가 없으면 생성
        if (exchangeAccountRepository.findByAccountNumber(desired).isEmpty()) {
            exchangeAccountService.createExchangeAccount(desired);
            log.info("Exchange account seeded: {}", desired);
        } else {
            log.info("Exchange account number already present: {}", desired);
        }
    }
}
