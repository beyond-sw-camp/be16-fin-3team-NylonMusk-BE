package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.TransactionEvent;
import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import com.beyond.MKX.domain.execution.repository.LedgerRepository;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 입출금 거래 이벤트 Consumer
 * 
 * - Kafka "transaction-events" 토픽에서 이벤트를 수신
 * - MEMBER, CORPORATION 계좌의 입금/출금을 Ledger에 기록
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {
    
    private final LedgerRepository ledgerRepository;
    private final MemberAccountRepository memberAccountRepository;

    @KafkaListener(
            topics = "transaction-events",
            groupId = "${spring.kafka.consumer.transaction-group-id}",
            containerFactory = "kafkaTransactionListenerFactory"
    )
    public void consumeTransactionEvent(
            @Payload TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment ack
    ) {
        try {
            log.info("=== Transaction Event 수신: partition={}, offset={}", partition, offset);
            log.info("TransactionEvent: {}", event);

            // TransactionType 매핑
            TransactionType transactionType;
            try {
                transactionType = TransactionType.valueOf(event.getTransactionType());
            } catch (IllegalArgumentException e) {
                log.error("유효하지 않은 TransactionType: {}", event.getTransactionType());
                ack.acknowledge();
                return;
            }
            
            // MEMBER 계좌 처리
            if ("MEMBER".equals(event.getAccountType())) {
                var accountOpt = memberAccountRepository.findByNumber(event.getAccountNumber());
                if (accountOpt.isEmpty()) {
                    log.warn("계좌를 찾을 수 없음: accountNumber={}", event.getAccountNumber());
                    ack.acknowledge();
                    return;
                }
                
                var account = accountOpt.get();
                // accountId를 event에서 받거나 DB 조회로 사용
                UUID accountId = event.getAccountId() != null && !event.getAccountId().isEmpty()
                    ? UUID.fromString(event.getAccountId())
                    : account.getId();
                UUID brokerageId = account.getBrokerageId();

                // 입금(DEPOSIT): null(시스템) → account (시스템에서 회원 계좌로 입금)
                // 출금(WITHDRAWAL): account → null(시스템) (회원 계좌에서 시스템으로 출금)
                // 이체(TRANSFER): 계좌간 이체 (상대방 정보 포함)
                UUID creditAccountId;
                UUID debitAccountId;
                Long debitAmount;
                Long creditAmount;
                
                if (transactionType == TransactionType.DEPOSIT) {
                    creditAccountId = accountId;    // 입금 받는 쪽: 회원 계좌
                    debitAccountId = null;           // 입금 보내는 쪽: 시스템(null)
                    debitAmount = 0L;
                    creditAmount = event.getAmount();
                } else if (transactionType == TransactionType.WITHDRAWAL) {
                    creditAccountId = null;          // 출금 받는 쪽: 시스템(null)
                    debitAccountId = accountId;      // 출금 보내는 쪽: 회원 계좌
                    debitAmount = event.getAmount();
                    creditAmount = 0L;
                } else if (transactionType == TransactionType.TRANSFER) {
                    // 이체는 method로 입금/출금 구분 (publishTransferDepositEvent는 입금, publishTransferWithdrawalEvent는 출금)
                    if ("TRANSFER_DEPOSIT".equals(event.getMethod()) || event.getMethod() == null) {
                        // 이체 입금: 다른 계좌 → 내 계좌
                        creditAccountId = accountId;
                        debitAccountId = null;
                        debitAmount = 0L;
                        creditAmount = event.getAmount();
                    } else {
                        // 이체 출금: 내 계좌 → 다른 계좌
                        creditAccountId = null;
                        debitAccountId = accountId;
                        debitAmount = event.getAmount();
                        creditAmount = 0L;
                    }
                } else {
                    creditAccountId = null;
                    debitAccountId = accountId;
                    debitAmount = event.getAmount();
                    creditAmount = 0L;
                }

                // Ledger 기록
                Ledger ledger = Ledger.builder()
                        .orderLogId(null)  // 입출금은 주문과 무관
                        .creditAccountId(creditAccountId)
                        .debitAccountId(debitAccountId)
                        .ticker("")  // 입출금은 ticker 없음 (빈 문자열)
                        .debit(debitAmount)
                        .credit(creditAmount)
                        .qtyChange(0L)  // 입출금은 거래량 없음
                        .amountChange(event.getAmount())  // 입출금 금액 = amountChange
                        .commission(0L)
                        .tax(0L)
                        .transactionType(transactionType)
                        .counterpartyAccountNumber(event.getCounterpartyAccountNumber())
                        .counterpartyName(event.getCounterpartyName())
                        .build();
                
                ledgerRepository.save(ledger);
                log.info("MEMBER Ledger 기록 완료: eventId={}, type={}, amount={}, counterparty={}", 
                        event.getEventId(), transactionType, event.getAmount(), 
                        event.getCounterpartyName() != null ? event.getCounterpartyName() : "N/A");
            } 
            // CORPORATION 계좌 처리
            else if ("CORPORATION".equals(event.getAccountType())) {
                // CORPORATION은 mkx-platform에서 accountId를 전달받음
                if (event.getAccountId() == null || event.getAccountId().isEmpty()) {
                    log.warn("CORPORATION 계좌 accountId가 없음: accountNumber={}, 이벤트 건너뜀", event.getAccountNumber());
                    ack.acknowledge();
                    return;
                }
                
                UUID corporationAccountId = UUID.fromString(event.getAccountId());
                
                // 입금(DEPOSIT): 거래소 → corporation (debitAccountId는 null)
                // 출금(WITHDRAWAL): corporation → 거래소 (creditAccountId는 null)
                UUID creditAccountId;
                UUID debitAccountId;
                
                if (transactionType == TransactionType.DEPOSIT) {
                    creditAccountId = corporationAccountId;  // 입금 받는 쪽: 기업 계좌
                    debitAccountId = null;                   // 입금 보내는 쪽: null (거래소 계좌 미사용)
                } else {
                    creditAccountId = null;                  // 출금 받는 쪽: null (거래소 계좌 미사용)
                    debitAccountId = corporationAccountId;   // 출금 보내는 쪽: 기업 계좌
                }
                
                // Ledger 기록
                Ledger ledger = Ledger.builder()
                        .orderLogId(null)
                        .creditAccountId(creditAccountId)
                        .debitAccountId(debitAccountId)
                        .ticker("")  // 입출금은 ticker 없음 (빈 문자열)
                        .debit(event.getAmount())
                        .credit(event.getAmount())
                        .qtyChange(0L)  // 입출금은 거래량 없음
                        .amountChange(event.getAmount())  // 입출금 금액 = amountChange
                        .commission(0L)
                        .tax(0L)
                        .transactionType(transactionType)
                        .build();
                
                ledgerRepository.save(ledger);
                log.info("CORPORATION Ledger 기록 완료: eventId={}, type={}, amount={}, accountId={}", 
                        event.getEventId(), transactionType, event.getAmount(), corporationAccountId);
            }

            ack.acknowledge();
            log.info("=== Transaction Event 처리 완료 ===");
            
        } catch (Exception e) {
            log.error("Transaction Event 처리 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}

