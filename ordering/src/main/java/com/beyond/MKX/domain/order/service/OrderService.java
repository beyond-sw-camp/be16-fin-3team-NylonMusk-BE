package com.beyond.MKX.domain.order.service;

import com.beyond.MKX.domain.account.member.service.DistributedLockService;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import com.beyond.MKX.domain.order.dto.CommissionAndTaxData;
import com.beyond.MKX.domain.order.dto.OrderCancelRequestDTO;
import com.beyond.MKX.domain.order.dto.OrderRequestDTO;
import com.beyond.MKX.domain.order.dto.OrderResponseDTO;
import com.beyond.MKX.domain.order.entity.OrderKind;
import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.entity.Side;
import com.beyond.MKX.domain.order.repository.OrderLogRepository;
import com.beyond.MKX.domain.outbox.entity.OrderOutbox;
import com.beyond.MKX.domain.outbox.entity.OrderPayload;
import com.beyond.MKX.domain.outbox.repository.OrderOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderValidatorService validator;
    private final FeePolicyService feePolicyService;
    private final OrderBookService orderBookService;
    private final MemberAccountRepository memberAccountRepository;
    private final OrderLogRepository orderLogRepository;
    private final OrderOutboxRepository outboxRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final ObjectMapper objectMapper;
    private final DistributedLockService distributedLockService;

    private static final BigDecimal PROTECTIVE_CAP_RATIO = new BigDecimal("0.05"); // 시장가 보호한도
    private static final int MAX_LOCK_RETRIES = 3; // 락 획득 최대 재시도 횟수
    private static final Duration LOCK_RETRY_DELAY = Duration.ofMillis(50); // 재시도 대기 시간

    // 주문 접수 서비스 로직
    public OrderResponseDTO placeOrder(OrderRequestDTO dto) {
        UUID accountId = dto.accountId();
        String ticker = dto.ticker();

        // 계좌별 분산 락 획득 (같은 계좌의 주문은 순차 처리)
        try (DistributedLockService.LockResource lock = distributedLockService.acquireLock(accountId)) {
            if (!lock.isAcquired()) {
                // 락 획득 실패 시 재시도
                return placeOrderWithRetry(dto, accountId, ticker);
            }
            
            // 락 획득 성공 시 주문 처리
            return doPlaceOrder(dto, accountId, ticker);
        }
    }

    /**
     * 락 획득 실패 시 재시도 로직
     */
    private OrderResponseDTO placeOrderWithRetry(OrderRequestDTO dto, UUID accountId, String ticker) {
        for (int i = 0; i < MAX_LOCK_RETRIES; i++) {
            try {
                Thread.sleep(LOCK_RETRY_DELAY.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("주문 처리 중단됨", e);
            }

            try (DistributedLockService.LockResource lock = distributedLockService.acquireLock(accountId)) {
                if (lock.isAcquired()) {
                    return doPlaceOrder(dto, accountId, ticker);
                }
            }
        }
        
        // 최대 재시도 후에도 락 획득 실패
        log.warn("락 획득 실패: accountId={}, 최대 재시도 횟수 초과", accountId);
        throw new IllegalStateException("다른 주문이 처리 중입니다. 잠시 후 다시 시도해주세요.");
    }

    /**
     * 실제 주문 처리 로직
     */
    private OrderResponseDTO doPlaceOrder(OrderRequestDTO dto, UUID accountId, String ticker) {

        // 0. 멱등성 검사
        /// TODO: 추후 멱등성 검사 도입 예정

        // 1. 검증 (비관적 잠금으로 동시성 제어)
        MemberAccount memberAccount = memberAccountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new EntityNotFoundException("해당 계좌가 존재하지 않습니다."));
        validator.validateAccount(memberAccount);
        validator.validateTradable(ticker);

        // 2. 예상 비용 계산
        // 2-1. 주문종류에 따른 가격 결정
        Long askingPrice = 0L;
        if (dto.orderKind() == OrderKind.MARKET) {
            if (dto.side() == Side.BUY) {
                // 오더북 레디스에서 최저가 조회
                askingPrice = orderBookService.getLowestAsk(ticker)
                        .orElseThrow(() -> new NoSuchElementException("매도 호가가 존재하지 않아 시장가 매수를 할 수 없습니다."));

                // 보호한도(CAP) 적용
                askingPrice = calculateAdjustedPrice(dto.side(), askingPrice);
            } else if (dto.side() == Side.SELL) {
                // 오더북 레디스에서 최고가 조회
                askingPrice = orderBookService.getHighestBid(ticker)
                        .orElseThrow(() -> new NoSuchElementException("매수 호가가 존재하지 않아 시장가 매도를 할 수 없습니다."));

                // 보호한도(CAP) 적용
                askingPrice = calculateAdjustedPrice(dto.side(), askingPrice);
            }
        } else if (dto.orderKind() == OrderKind.LIMIT) {
            askingPrice = dto.price();
        }

        // 2-2. 수수료, 세글 및 동결할 총액을 계산
        long notionalValue = Math.multiplyExact(askingPrice, dto.quantity()); // 대금 계산
        Long brokerageCommission;
        Long transactionTax = null;
        Long totalAmount;

        // 수수료 계산
        if (dto.side() == Side.BUY) {
            brokerageCommission = feePolicyService.estimateBidFee(notionalValue, memberAccount.getBrokerageId());
            totalAmount = Math.addExact(brokerageCommission, notionalValue);
        } else {
            CommissionAndTaxData commissionAndTaxData = feePolicyService.estimateAckFee(notionalValue, memberAccount.getBrokerageId());
            brokerageCommission = commissionAndTaxData.getCommission();
            transactionTax = commissionAndTaxData.getTax();
            long feeTotalAmount = Math.addExact(brokerageCommission, transactionTax);
            totalAmount = Math.addExact(notionalValue, feeTotalAmount);
        }

        // 3. 자산 동결
        // (매수) 계좌 금액 동결
        if (dto.side() == Side.BUY) {
            if (memberAccount.getAvailableBalance() < totalAmount) {
                throw new IllegalArgumentException("계좌의 잔고가 부족합니다.");
            }
            memberAccount.decreaseAvailableBalance(totalAmount);
        } else { // (매도) 보유주식 수 동결 (비관적 잠금으로 동시성 제어)
            StockHolding stockHolding = stockHoldingRepository.findByMemberAccountIdAndTickerWithLock(accountId, ticker)
                    .orElseThrow(() -> new EntityNotFoundException("해당 보유 주식이 존재하지 않습니다."));
            if (stockHolding.getAvailableQuantity() < dto.quantity()) {
                throw new IllegalArgumentException("매도 가능한 주식 수량이 부족합니다.");
            }
            stockHolding.decreaseAvailableQuantity(dto.quantity());
        }

        // 4. 주문 기록
        OrderLog order = OrderLog.builder()
                .account(memberAccount)
                .brokerageId(memberAccount.getBrokerageId())
                .ticker(ticker)
                .orderKind(dto.orderKind())
                .side(dto.side())
                .price(askingPrice)
                .quantity(dto.quantity())
                .commission(brokerageCommission)
                .transactionTax(transactionTax)
                .remainQuantity(dto.quantity())
                .build();
        if (order.getSide() == Side.BUY) {
            order.insertOrderFreezeAmount(totalAmount);
        }
        orderLogRepository.save(order);

        // 5. 아웃 박스 기록
        insertOrderOutbox(
                OrderPayload.builder()
                        .brokerageId(order.getBrokerageId())
                        .orderId(order.getId())
                        .ticker(order.getTicker())
                        .side(order.getSide())
                        .orderKind(order.getOrderKind())
                        .price(order.getPrice())
                        .quantity(order.getQuantity())
                        .accountId(accountId)
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        // 6. Ack 생성
        return OrderResponseDTO.from(order);
    }

    // 주문 취소 서비스 로직
    public OrderResponseDTO cancelOrder(OrderCancelRequestDTO dto, UUID memberId) {
        MemberAccount memberAccount = memberAccountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new EntityNotFoundException("해당 계좌를 찾을 수 없습니다."));

        OrderLog orderLog = orderLogRepository.findByIdAndAccount_Id(dto.orderLogId(), memberAccount.getId())
                .orElseThrow(() -> new EntityNotFoundException("해당 주문기록을 찾을 수 없습니다."));

        insertOrderOutbox(
                OrderPayload.builder()
                        .orderId(orderLog.getId())
                        .ticker(orderLog.getTicker())
                        .side(orderLog.getSide())
                        .orderKind(OrderKind.CANCEL)
                        .accountId(memberAccount.getId())
                        .build()
        );
        return OrderResponseDTO.from(orderLog);
    }


    /// **-------------- 내부 메서드들 --------------**
    // outbox 생성 및 기록 메서드
    private void insertOrderOutbox(OrderPayload orderPayload) {
        try {
            // DTO 직렬화
            String payloadJson = objectMapper.writeValueAsString(orderPayload);

            // Outbox 엔티티 생성 및 저장
            OrderOutbox orderOutbox = OrderOutbox.builder()
                    .orderLogId(orderPayload.getOrderId())
                    .eventType("ORDER_PLACED")
                    .kafkaKey(orderPayload.getTicker())  // ticker를 key로 사용하여 같은 종목의 주문 순서 보장 (매칭 엔진)
                    .payload(payloadJson)
                    .build();
            outboxRepository.save(orderOutbox);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    // 보호 한도 계산 메서드
    private Long calculateAdjustedPrice(Side side, Long price) {
        BigDecimal originalPrice = new BigDecimal(price);
        BigDecimal bufferFactor = switch (side) {
            case BUY -> BigDecimal.ONE.add(PROTECTIVE_CAP_RATIO); // 1 + 보호한도
            case SELL -> BigDecimal.ONE.subtract(PROTECTIVE_CAP_RATIO); // 1 - 보호한도
        };
        // 계산된 값을 Long으로 변환하여 반환 (소수점 이하는 버림)
        return originalPrice.multiply(bufferFactor).longValue();
    }

}
