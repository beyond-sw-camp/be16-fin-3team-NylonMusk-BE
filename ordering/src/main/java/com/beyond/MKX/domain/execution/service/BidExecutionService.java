package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import com.beyond.MKX.domain.execution.entity.FillLog;
import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import com.beyond.MKX.domain.execution.repository.FillLogRepository;
import com.beyond.MKX.domain.execution.repository.LedgerRepository;
import com.beyond.MKX.domain.order.entity.OrderKind;
import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.entity.OrderStatus;
import com.beyond.MKX.domain.order.entity.Side;
import com.beyond.MKX.domain.order.repository.OrderLogRepository;
import com.beyond.MKX.domain.order.service.FeePolicyService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class BidExecutionService {

    private final FillLogRepository fillLogRepository;
    private final OrderLogRepository orderLogRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final FeePolicyService feePolicyService;
    private final LedgerRepository ledgerRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public BidExecutionService(FillLogRepository fillLogRepository,
                               OrderLogRepository orderLogRepository,
                               StockHoldingRepository stockHoldingRepository,
                               FeePolicyService feePolicyService,
                               LedgerRepository ledgerRepository,
                               @Qualifier("idempotency") RedisTemplate<String, String> redisTemplate) {
        this.fillLogRepository = fillLogRepository;
        this.orderLogRepository = orderLogRepository;
        this.stockHoldingRepository = stockHoldingRepository;
        this.feePolicyService = feePolicyService;
        this.ledgerRepository = ledgerRepository;
        this.redisTemplate = redisTemplate;
    }

    // 매수자 체결 후처리 로직
    public boolean bidExecuteProcess(UUID bidOrderId, ExecutionEvent executionEvent) {

        /// 0. 멱등성 검사
        boolean b = fillLogRepository.existsByOrderLogIdAndExecId(bidOrderId, executionEvent.getExecId());
        if (b) {
            return false;
        }

        /// 1. 체결 로그
        fillLogRepository.save(
                FillLog.builder()
                        .orderLogId(bidOrderId)
                        .execId(executionEvent.getExecId())
                        .ticker(executionEvent.getTicker())
                        .side(Side.BUY)
                        .price(executionEvent.getPrice())
                        .quantity(executionEvent.getQuantity())
                        .build()
        );

        /// 2. 기본 엔티티 가져오기
        OrderLog orderLog = orderLogRepository.findById(bidOrderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 주문기록이 없습니다."));
        MemberAccount memberAccount = orderLog.getAccount();

        /// 3. 보유 주식 반영
        Optional<StockHolding> stockHoldingOpt = stockHoldingRepository
                .findByMemberAccountIdAndTicker(memberAccount.getId(), executionEvent.getTicker());
        if (stockHoldingOpt.isPresent()) {
            // 기존에 존재시 update
            StockHolding stockHolding = stockHoldingOpt.get();
            stockHolding.increaseTotalQuantity(executionEvent.getQuantity());
            stockHolding.increaseAvaQuantity(executionEvent.getQuantity());
            stockHolding.incTotalPurchasePrice(executionEvent.getQuantity(), executionEvent.getPrice());
        } else {
            // 기존에 보유하고 있지 않다면 새로 생성
            StockHolding stockHolding = StockHolding.builder()
                    .memberAccountId(memberAccount.getId())
                    .brokerageId(orderLog.getBrokerageId())
                    .ticker(orderLog.getTicker())
                    .totalQuantity(executionEvent.getQuantity())
                    .availableQuantity(executionEvent.getQuantity())
                    .totalPurchasePrice(Math.multiplyExact(executionEvent.getPrice(), executionEvent.getQuantity()))
                    .build();
            stockHoldingRepository.save(stockHolding);
        }

        /// 4. 돈 계산(체결 단위)
        // 체결 이벤트 값으로 총체결금액, 수수로, 거래금액 계산.
        // 절대 OrderLog의 기록된 값이 아닌 체결 이벤트의 값으로 계산하기.
        long transactionAmount = Math.multiplyExact(executionEvent.getPrice(), executionEvent.getQuantity());
        Long commission = feePolicyService.estimateBidFee(transactionAmount, memberAccount.getBrokerageId());
        Long total_filled_amount = Math.addExact(transactionAmount, commission);

        // 계좌 출금
        long l = memberAccount.decreaseBalance(total_filled_amount);
        log.info("전체 잔고 {}원 차감, 남은 금액: {}원", total_filled_amount, l);
        if (l < 0L) {
            log.warn("체결 후 출금 계좌 잔고 부족. 계좌 ID: {}, 잔고: {}", memberAccount.getId(), l);
            // TODO: 계좌 잔고 부족 시 미수금 처리 및 알림 발송 (카프카 발행)
        }

        /// 5. 주문 상태 변경
        orderLog.updateOrderStatus(OrderStatus.PARTIALLY_FILLED);
        orderLog.recordFilledAt();
        orderLog.decFreezeAmount(total_filled_amount); // 동결 금액 차감
        log.info("{}원 동결 금액 차감", total_filled_amount);
        // 잔여 수량 감소
        long remainQuantity = orderLog.decRemainQuantity(executionEvent.getQuantity());
        if (remainQuantity == 0L) {
            /**
             * == 전액 체결 시 로직 ==
             * 지정가 주문 시 order_log 테이블에 남은 수량이 0인지 확인 후 환불 진행
             */
            // 5-1. order_log 상태 변경
            orderLog.updateOrderStatus(OrderStatus.FILLED);

            // 5-2. 환불 처리 (계좌 가용 금액 증가)
            System.out.println("bidExecuteProcess: 환불 로직 시작");
            memberAccount.increaseAvailableBalance(orderLog.getFreezeAmount());
            log.info("{}원 환불 처리", orderLog.getFreezeAmount());
        } else if (orderLog.getOrderKind() == OrderKind.MARKET) {
            Long added = redisTemplate.opsForSet().add("order-id:" + orderLog.getId(), String.valueOf(remainQuantity));
            if (added != null && added == 0) {
                System.out.println("bidExecuteProcess: 환불 로직 시작");
                memberAccount.increaseAvailableBalance(orderLog.getFreezeAmount());
                log.info("시장가 {}원 환불 처리", orderLog.getFreezeAmount());
            }

        }

        /// 6. 원장 기록
        Ledger ledger = Ledger.builder()
                .orderLogId(orderLog.getId())
                .creditAccountId(memberAccount.getId())
                .debitAccountId(memberAccount.getBrokerageId())
                .ticker(executionEvent.getTicker())
                .debit(transactionAmount)
                .credit(total_filled_amount)
                .qtyChange(executionEvent.getQuantity())
                .amountChange(executionEvent.getPrice())
                .commission(commission)
                .transactionType(TransactionType.BUY)
                .build();
        ledgerRepository.save(ledger);
        // TODO: 카프카 발행 및 원장 서비스 모듈 분리

        return true;
    }


}
