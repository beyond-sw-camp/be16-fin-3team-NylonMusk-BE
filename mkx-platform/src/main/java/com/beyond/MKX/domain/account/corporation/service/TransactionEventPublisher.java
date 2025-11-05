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

    /**
     * 입금 이벤트 발행 (TransactionType 및 상대 계좌번호 포함)
     * 
     * @param accountId 계좌 UUID
     * @param accountNumber 계좌번호
     * @param amount 거래 금액
     * @param transactionType 거래 유형 (IPO_REFUND, IPO_PAYOUT 등)
     * @param counterpartyAccountNumber 상대 계좌번호 (선택적)
     */
    public void publishDepositEventWithType(String accountId, String accountNumber, Long amount, 
                                            String transactionType, String counterpartyAccountNumber) {
        publishDepositEventWithType(accountId, accountNumber, amount, transactionType, counterpartyAccountNumber, null);
    }

    /**
     * 출금 이벤트 발행 (TransactionType 및 상대 계좌번호 포함)
     * 
     * @param accountId 계좌 UUID
     * @param accountNumber 계좌번호
     * @param amount 거래 금액
     * @param transactionType 거래 유형 (IPO_PAID, IPO_ADDITIONAL 등)
     * @param counterpartyAccountNumber 상대 계좌번호 (선택적)
     */
    public void publishWithdrawalEventWithType(String accountId, String accountNumber, Long amount, 
                                               String transactionType, String counterpartyAccountNumber) {
        publishWithdrawalEventWithType(accountId, accountNumber, amount, transactionType, counterpartyAccountNumber, null);
    }
    
    /**
     * 출금 이벤트 발행 (TransactionType, 상대 계좌번호, ticker 포함)
     * 
     * @param accountId 계좌 UUID
     * @param accountNumber 계좌번호
     * @param amount 거래 금액
     * @param transactionType 거래 유형 (IPO_PAID, IPO_ADDITIONAL 등)
     * @param counterpartyAccountNumber 상대 계좌번호 (선택적)
     * @param ticker 종목 코드 (선택적, IPO 관련 거래 시 사용)
     */
    public void publishWithdrawalEventWithType(String accountId, String accountNumber, Long amount, 
                                               String transactionType, String counterpartyAccountNumber, String ticker) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .accountNumber(accountNumber)
                .accountId(accountId)
                .accountType("CORPORATION")
                .transactionType(transactionType)  // 파라미터로 받은 transactionType 사용
                .amount(amount)
                .method(transactionType)
                .counterpartyAccountNumber(counterpartyAccountNumber)  // 상대 계좌번호 추가
                .ticker(ticker)  // 종목 코드 추가
                .timestamp(System.currentTimeMillis())
                .build();
        
        transactionKafkaTemplate.send(TOPIC, accountNumber, event);
        log.info("출금 이벤트 발행: accountNumber={}, accountId={}, amount={}, transactionType={}, counterparty={}, ticker={}", 
                accountNumber, accountId, amount, transactionType, counterpartyAccountNumber, ticker);
    }
    
    /**
     * 입금 이벤트 발행 (TransactionType, 상대 계좌번호, ticker 포함)
     * 
     * @param accountId 계좌 UUID
     * @param accountNumber 계좌번호
     * @param amount 거래 금액
     * @param transactionType 거래 유형 (IPO_REFUND, IPO_PAYOUT 등)
     * @param counterpartyAccountNumber 상대 계좌번호 (선택적)
     * @param ticker 종목 코드 (선택적, IPO 관련 거래 시 사용)
     */
    public void publishDepositEventWithType(String accountId, String accountNumber, Long amount, 
                                            String transactionType, String counterpartyAccountNumber, String ticker) {
        TransactionEvent event = TransactionEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .accountNumber(accountNumber)
                .accountId(accountId)
                .accountType("CORPORATION")
                .transactionType(transactionType)  // 파라미터로 받은 transactionType 사용
                .amount(amount)
                .method(transactionType)
                .counterpartyAccountNumber(counterpartyAccountNumber)  // 상대 계좌번호 추가
                .ticker(ticker)  // 종목 코드 추가
                .timestamp(System.currentTimeMillis())
                .build();
        
        transactionKafkaTemplate.send(TOPIC, accountNumber, event);
        log.info("입금 이벤트 발행: accountNumber={}, accountId={}, amount={}, transactionType={}, counterparty={}, ticker={}", 
                accountNumber, accountId, amount, transactionType, counterpartyAccountNumber, ticker);
    }
}

