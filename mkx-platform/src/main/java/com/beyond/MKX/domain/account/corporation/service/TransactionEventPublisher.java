package com.beyond.MKX.domain.account.corporation.service;

import com.beyond.MKX.common.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 입출금 거래 이벤트 발행 서비스 (CORPORATION)
 * 
 * - CORPORATION 계좌의 입금/출금 이벤트를 Kafka로 발행
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TransactionEventPublisher {
    
    private final KafkaTemplate<String, TransactionEvent> transactionKafkaTemplate;
    private static final String TOPIC = "transaction-events";

    /**
     * 입금 이벤트 발행
     * 
     * @param accountId 계좌 UUID
     * @param accountNumber 계좌번호
     * @param amount 거래 금액
     * @param method 거래 방법
     */
    public void publishDepositEvent(String accountId, String accountNumber, Long amount, String method) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .accountNumber(accountNumber)
                .accountId(accountId)
                .accountType("CORPORATION")
                .transactionType("DEPOSIT")
                .amount(amount)
                .method(method)
                .timestamp(System.currentTimeMillis())
                .build();
        
        transactionKafkaTemplate.send(TOPIC, accountNumber, event);
        log.info("입금 이벤트 발행: accountNumber={}, accountId={}, amount={}", accountNumber, accountId, amount);
    }

    /**
     * 출금 이벤트 발행
     * 
     * @param accountId 계좌 UUID
     * @param accountNumber 계좌번호
     * @param amount 거래 금액
     * @param method 거래 방법
     */
    public void publishWithdrawalEvent(String accountId, String accountNumber, Long amount, String method) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .accountNumber(accountNumber)
                .accountId(accountId)
                .accountType("CORPORATION")
                .transactionType("WITHDRAWAL")
                .amount(amount)
                .method(method)
                .timestamp(System.currentTimeMillis())
                .build();
        
        transactionKafkaTemplate.send(TOPIC, accountNumber, event);
        log.info("출금 이벤트 발행: accountNumber={}, accountId={}, amount={}", accountNumber, accountId, amount);
    }
}

