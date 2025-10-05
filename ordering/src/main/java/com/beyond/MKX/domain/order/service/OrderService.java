package com.beyond.MKX.domain.order.service;

import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import com.beyond.MKX.domain.order.dto.OrderRequestDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderValidatorService validator;
    private final MemberAccountRepository memberAccountRepository;
    private final FeePolicyService feePolicyService;

    public void placeOrder(OrderRequestDTO dto) {
        UUID accountId = dto.accountId();
        String ticker = dto.ticker();

        // 멱등성 검사
        /// TODO: 추후 멱등성 검사 도입 예정

        // 검증
        MemberAccount memberAccount = memberAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("해당 계좌가 존재하지 않습니다."));
        validator.validateAccount(memberAccount);
        validator.validateTradable(ticker);

        // 예상 비용 계산


        // 금액 동결


        // 주문 생성


        // 아웃 박스 기록


        // Ack 생성


    }
}
