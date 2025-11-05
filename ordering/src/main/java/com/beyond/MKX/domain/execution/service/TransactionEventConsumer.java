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

                // 입금/출금 이중 기록 (Double-Entry)
                // DB 제약조건: credit_account_id와 debit_account_id 모두 NOT NULL
                // 따라서 null 방지를 위해 accountId를 사용
                UUID creditAccountId;
                UUID debitAccountId;
                Long debitAmount;
                Long creditAmount;
                
                if (transactionType == TransactionType.DEPOSIT 
                        || transactionType == TransactionType.DELISTING_REFUND
                        || transactionType == TransactionType.ORDER_REFUND
                        || transactionType == TransactionType.IPO_PAYOUT
                        || transactionType == TransactionType.IPO_REFUND) {
                    // 입금 또는 상장폐지 환불 또는 주문 환불 또는 IPO 환불/송금: 시스템 → 회원 계좌
                    creditAccountId = accountId;    // 입금 받는 쪽: 회원 계좌
                    debitAccountId = accountId;      // 입금 보내는 쪽도 회원 계좌로 설정 (null 방지)
                    debitAmount = 0L;
                    creditAmount = event.getAmount();
                } else if (transactionType == TransactionType.WITHDRAWAL
                        || transactionType == TransactionType.IPO_PAID
                        || transactionType == TransactionType.IPO_ADDITIONAL) {
                    // 출금 또는 IPO 납입/추가납입: 회원 계좌 → 시스템
                    creditAccountId = accountId;    // 출금 받는 쪽도 회원 계좌로 설정 (null 방지)
                    debitAccountId = accountId;      // 출금 보내는 쪽: 회원 계좌
                    debitAmount = event.getAmount();
                    creditAmount = 0L;
                } else if (transactionType == TransactionType.TRANSFER) {
                    // 이체는 method로 입금/출금 구분 (publishTransferDepositEvent는 입금, publishTransferWithdrawalEvent는 출금)
                    if ("TRANSFER_DEPOSIT".equals(event.getMethod()) || event.getMethod() == null) {
                        // 이체 입금: 다른 계좌 → 내 계좌
                        creditAccountId = accountId;
                        debitAccountId = accountId;  // null 방지
                        debitAmount = 0L;
                        creditAmount = event.getAmount();
                    } else {
                        // 이체 출금: 내 계좌 → 다른 계좌
                        creditAccountId = accountId;  // null 방지
                        debitAccountId = accountId;
                        debitAmount = event.getAmount();
                        creditAmount = 0L;
                    }
                } else {
                    // 기타 타입: 기본값 (둘 다 회원 계좌로 설정)
                    creditAccountId = accountId;
                    debitAccountId = accountId;
                    debitAmount = event.getAmount();
                    creditAmount = 0L;
                }

                // Ledger 기록
                // ⭐ 상장폐지 환불 또는 IPO 관련 거래인 경우 ticker 정보 포함
                String ticker = "";
                if (event.getTicker() != null && !event.getTicker().isEmpty()) {
                    // IPO 관련 거래 또는 상장폐지 환불인 경우 ticker 저장
                    if (transactionType == TransactionType.DELISTING_REFUND
                            || transactionType == TransactionType.IPO_PAID
                            || transactionType == TransactionType.IPO_REFUND
                            || transactionType == TransactionType.IPO_ADDITIONAL
                            || transactionType == TransactionType.IPO_PAYOUT) {
                        ticker = event.getTicker();
                    }
                }
                Long qtyChange = (transactionType == TransactionType.DELISTING_REFUND && event.getQuantity() != null) 
                        ? event.getQuantity() : 0L;
                
                // orderLogId는 DB 제약조건상 NOT NULL이므로 eventId를 UUID로 변환하여 사용
                UUID orderLogId = UUID.fromString(event.getEventId());
                
                Ledger ledger = Ledger.builder()
                        .orderLogId(orderLogId)  // eventId를 UUID로 변환하여 사용 (입출금은 주문과 무관하지만 DB 제약조건 만족)
                        .creditAccountId(creditAccountId)
                        .debitAccountId(debitAccountId)
                        .ticker(ticker)  // IPO 관련 거래 또는 상장폐지 환불인 경우 ticker 기록
                        .debit(debitAmount)
                        .credit(creditAmount)
                        .qtyChange(qtyChange)  // 상장폐지 환불인 경우 주식 수량 기록
                        .amountChange(event.getAmount())  // 입출금 금액 = amountChange
                        .commission(0L)
                        .tax(0L)
                        .transactionType(transactionType)
                        .counterpartyAccountNumber(event.getCounterpartyAccountNumber())
                        .counterpartyName(event.getCounterpartyName())
                        .build();
                
                ledgerRepository.save(ledger);
                
                if (transactionType == TransactionType.DELISTING_REFUND) {
                    log.info("MEMBER Ledger 기록 완료 (상장폐지 환불): eventId={}, ticker={}, quantity={}주, price={}원, totalAmount={}", 
                            event.getEventId(), event.getTicker(), event.getQuantity(), 
                            event.getPricePerShare(), event.getAmount());
                } else {
                    log.info("MEMBER Ledger 기록 완료: eventId={}, type={}, amount={}, counterparty={}", 
                            event.getEventId(), transactionType, event.getAmount(), 
                            event.getCounterpartyName() != null ? event.getCounterpartyName() : "N/A");
                }
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
                
                // 입금/출금 이중 기록 (Double-Entry)
                // IPO 타입별 처리:
                // - IPO_PAYOUT, IPO_REFUND: 입금 (거래소/증권사 → 기업 계좌)
                // - IPO_PAID, IPO_ADDITIONAL: 출금 (기업 계좌 → 증권사 예치 계좌)
                UUID creditAccountId;
                UUID debitAccountId;
                Long debitAmount;
                Long creditAmount;
                
                if (transactionType == TransactionType.DEPOSIT 
                        || transactionType == TransactionType.DELISTING_REFUND
                        || transactionType == TransactionType.ORDER_REFUND
                        || transactionType == TransactionType.IPO_PAYOUT
                        || transactionType == TransactionType.IPO_REFUND) {
                    // 입금: 거래소/시스템 → 기업 계좌
                    creditAccountId = corporationAccountId;  // 입금 받는 쪽: 기업 계좌
                    debitAccountId = corporationAccountId;   // 입금 보내는 쪽도 기업 계좌로 설정 (null 방지)
                    debitAmount = 0L;
                    creditAmount = event.getAmount();
                } else if (transactionType == TransactionType.WITHDRAWAL
                        || transactionType == TransactionType.IPO_PAID
                        || transactionType == TransactionType.IPO_ADDITIONAL) {
                    // 출금: 기업 계좌 → 거래소/증권사
                    creditAccountId = corporationAccountId;  // 출금 받는 쪽도 기업 계좌로 설정 (null 방지)
                    debitAccountId = corporationAccountId;   // 출금 보내는 쪽: 기업 계좌
                    debitAmount = event.getAmount();
                    creditAmount = 0L;
                } else {
                    // 기타 타입: 기본값 (둘 다 기업 계좌로 설정)
                    creditAccountId = corporationAccountId;
                    debitAccountId = corporationAccountId;
                    debitAmount = event.getAmount();
                    creditAmount = 0L;
                }
                
                // Ledger 기록
                // ⭐ 상장폐지 환불인 경우 ticker와 quantity 정보 포함
                String ticker = (transactionType == TransactionType.DELISTING_REFUND && event.getTicker() != null) 
                        ? event.getTicker() : "";
                Long qtyChange = (transactionType == TransactionType.DELISTING_REFUND && event.getQuantity() != null) 
                        ? event.getQuantity() : 0L;
                
                // orderLogId는 DB 제약조건상 NOT NULL이므로 eventId를 UUID로 변환하여 사용
                UUID orderLogId = UUID.fromString(event.getEventId());
                
                Ledger ledger = Ledger.builder()
                        .orderLogId(orderLogId)  // eventId를 UUID로 변환하여 사용 (입출금은 주문과 무관하지만 DB 제약조건 만족)
                        .creditAccountId(creditAccountId)
                        .debitAccountId(debitAccountId)
                        .ticker(ticker)  // 상장폐지 환불인 경우 ticker 기록
                        .debit(debitAmount)
                        .credit(creditAmount)
                        .qtyChange(qtyChange)  // 상장폐지 환불인 경우 주식 수량 기록
                        .amountChange(event.getAmount())  // 입출금 금액 = amountChange
                        .commission(0L)
                        .tax(0L)
                        .transactionType(transactionType)
                        .counterpartyAccountNumber(event.getCounterpartyAccountNumber())  // 상대 계좌번호 추가
                        .counterpartyName(event.getCounterpartyName())  // 상대 이름 추가
                        .build();
                
                ledgerRepository.save(ledger);
                
                if (transactionType == TransactionType.DELISTING_REFUND) {
                    log.info("CORPORATION Ledger 기록 완료 (상장폐지 환불): eventId={}, ticker={}, quantity={}주, price={}원, totalAmount={}", 
                            event.getEventId(), event.getTicker(), event.getQuantity(), 
                            event.getPricePerShare(), event.getAmount());
                } else {
                    log.info("CORPORATION Ledger 기록 완료: eventId={}, type={}, amount={}, accountId={}", 
                            event.getEventId(), transactionType, event.getAmount(), corporationAccountId);
                }
            }

            ack.acknowledge();
            log.info("=== Transaction Event 처리 완료 ===");
            
        } catch (Exception e) {
            log.error("Transaction Event 처리 실패: {}", e.getMessage(), e);
            throw e;
        }
    }
}

