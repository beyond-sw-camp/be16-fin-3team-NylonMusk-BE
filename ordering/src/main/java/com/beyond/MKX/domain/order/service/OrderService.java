package com.beyond.MKX.domain.order.service;

import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import com.beyond.MKX.domain.order.dto.OrderRequestDTO;
import com.beyond.MKX.domain.order.dto.OrderResponseDTO;
import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.outbox.OrderOutbox;
import com.beyond.MKX.domain.order.outbox.OrderOutboxRepository;
import com.beyond.MKX.domain.order.outbox.OrderPayload;
import com.beyond.MKX.domain.order.repository.OrderLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderValidatorService validator;
    private final MemberAccountRepository memberAccountRepository;
    private final FeePolicyService feePolicyService;
    private final OrderLogRepository orderLogRepository;
    private final OrderOutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;


    public OrderResponseDTO placeOrder(OrderRequestDTO dto) {
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
        long transactionAmount = Math.multiplyExact(dto.price(), dto.quantity());
        Long fee = feePolicyService.estimateAckFee(transactionAmount, memberAccount.getBrokerageId());
        Long totalAmount = Math.addExact(fee, transactionAmount);

        // 금액 동결
        if (memberAccount.getAvailableBalance() < totalAmount) {
            throw new IllegalArgumentException("계좌의 잔고가 부족합니다.");
        }
        memberAccount.decreaseAvailableBalance(totalAmount);

        // 주문 기록
        OrderLog order = OrderLog.builder()
                .account(memberAccount)
                .brokerageId(memberAccount.getBrokerageId())
                .ticker(ticker)
                .orderKind(dto.orderKind())
                .side(dto.side())
                .price(dto.price())
                .quantity(dto.quantity())
                .fee(fee)
                .totalAmount(totalAmount)
                .remainQuantity(dto.quantity())
                .build();
        orderLogRepository.save(order);

        // 아웃 박스 기록
        recordOrderOutbox(order);

        // Ack 생성
        return OrderResponseDTO.from(order);
    }

    private void recordOrderOutbox(OrderLog order) {
        try {
            // Kafka 메시지 DTO 생성
            OrderPayload orderPayload = OrderPayload.builder()
                    .brokerageId(order.getBrokerageId())
                    .orderId(order.getId())
                    .ticker(order.getTicker())
                    .side(order.getSide())
                    .orderKind(order.getOrderKind())
                    .price(order.getPrice())
                    .quantity(order.getQuantity())
                    .createdAt(LocalDateTime.now())
                    .build();

            // DTO 직렬화
            String payloadJson = objectMapper.writeValueAsString(orderPayload);
            
            // Outbox 엔티티 생성 및 저장
            OrderOutbox orderOutbox = OrderOutbox.builder()
                    .orderLogId(order.getId())
                    .eventType("ORDER_PLACED")
                    .payload(payloadJson)
                    .build();
            outboxRepository.save(orderOutbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
