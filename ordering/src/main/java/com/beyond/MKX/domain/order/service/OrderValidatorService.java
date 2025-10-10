package com.beyond.MKX.domain.order.service;

import com.beyond.MKX.domain.assets.entity.AccountStatus;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import org.springframework.stereotype.Component;

@Component
public class OrderValidatorService {

    // 종목 검증
    public void validateTradable(String ticker) {
        ///  TODO: Redis 오더북에서 키값을 가져와 검증 예정.
    }

    // 계좌 검증
    public void validateAccount(MemberAccount memberAccount) {
        AccountStatus status = memberAccount.getStatus();
        if (status == AccountStatus.PENDING)
            throw new IllegalStateException("현재 계좌는 '승인 대기' 중인 계좌입니다.");
        if (status == AccountStatus.SUSPENDED)
            throw new IllegalStateException("현재 계좌는 '거래 중지' 계좌입니다.");
    }



}
