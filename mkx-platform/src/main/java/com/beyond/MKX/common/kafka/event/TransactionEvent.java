package com.beyond.MKX.common.kafka.event;

import lombok.*;

/**
 * 입출금 거래 이벤트
 * 
 * 사용처
 * - 입금/출금 발생 시 Kafka "transaction-events" 토픽으로 전송하는 페이로드 모델
 * - ordering-service가 이를 컨슘하여 Ledger에 기록
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class TransactionEvent {
    private String eventId;              // 멱등성 키
    private String accountNumber;        // 계좌번호
    private String accountId;            // 계좌 UUID (MEMBER: MemberAccount.id, CORPORATION: CorporationAccount.id)
    private String accountType;          // MEMBER, CORPORATION, BROKERAGE, EXCHANGE
    private String transactionType;      // DEPOSIT, WITHDRAWAL
    private Long amount;                 // 거래 금액
    private String method;               // 거래 방법 (BANK_TRANSFER, CARD 등) - optional
    private String description;          // 거래 설명 - optional
    private Long timestamp;              // 이벤트 발생 시각
    
    // 상장폐지/IPO 환불 시 추가 정보
    private String ticker;               // 종목 코드
    private Long quantity;               // 주식 수량
    private Long pricePerShare;          // 주당 가격
}

