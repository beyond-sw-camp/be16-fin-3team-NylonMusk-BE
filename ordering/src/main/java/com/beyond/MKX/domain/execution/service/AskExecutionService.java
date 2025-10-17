package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import com.beyond.MKX.domain.execution.entity.FillLog;
import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.repository.FillLogRepository;
import com.beyond.MKX.domain.execution.repository.LedgerRepository;
import com.beyond.MKX.domain.order.dto.CommissionAndTaxData;
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
public class AskExecutionService {

    private final FillLogRepository fillLogRepository;
    private final OrderLogRepository orderLogRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final FeePolicyService feePolicyService;
    private final LedgerRepository ledgerRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public AskExecutionService(FillLogRepository fillLogRepository,
                               OrderLogRepository orderLogRepository,
                               StockHoldingRepository stockHoldingRepository,
                               FeePolicyService feePolicyService,
                               LedgerRepository ledgerRepository,
                               @Qualifier("idempotency") RedisTemplate<String, String> redisTemplate
    ) {
        this.fillLogRepository = fillLogRepository;
        this.orderLogRepository = orderLogRepository;
        this.stockHoldingRepository = stockHoldingRepository;
        this.feePolicyService = feePolicyService;
        this.ledgerRepository = ledgerRepository;
        this.redisTemplate = redisTemplate;
    }

    public boolean askExecuteProcess(UUID askOrderId, ExecutionEvent executionEvent) {

        // 0. 멱등성 검사
        boolean b = fillLogRepository.existsByOrderLogIdAndExecId(askOrderId, executionEvent.getExecId());
        if (b) {
            return true;
        }

        /// 1. 체결 로그
        fillLogRepository.save(
                FillLog.builder()
                        .orderLogId(askOrderId)
                        .execId(executionEvent.getExecId())
                        .ticker(executionEvent.getTicker())
                        .side(Side.SELL)
                        .price(executionEvent.getPrice())
                        .quantity(executionEvent.getQuantity())
                        .build()
        );

        /// 2. 기본 엔티티 가져오기
        OrderLog orderLog = orderLogRepository.findById(askOrderId)
                .orElseThrow(() -> new EntityNotFoundException("해당 주문기록이 없습니다."));
        MemberAccount memberAccount = orderLog.getAccount();

        /// 3. 보유 주식 반영
        StockHolding stockHolding = stockHoldingRepository
                .findByMemberAccountIdAndTicker(memberAccount.getId(), executionEvent.getTicker())
                .orElseThrow(() -> new EntityNotFoundException("해당 보유 주식이 존재하지 않습니다."));
        stockHolding.decreaseTotalQuantity(executionEvent.getQuantity());
        log.info("보유 주식 {}개 감소", executionEvent.getQuantity());
        stockHolding.decTotalPurchasePrice(executionEvent.getQuantity(), executionEvent.getPrice());

        /// 4. 돈 계산(체결 단위)
        // 체결 이벤트 값으로 총체결금액, 수수로, 거래금액 계산.
        // 절대 OrderLog의 기록된 값이 아닌 체결 이벤트의 값으로 계산하기.
        long notionalValue = Math.multiplyExact(executionEvent.getPrice(), executionEvent.getQuantity());
        CommissionAndTaxData commissionAndTaxData = feePolicyService
                .estimateAckFee(notionalValue, memberAccount.getBrokerageId());
        System.out.println("AskExecutionService.askExecuteProcess");
        System.out.println("======== 체결 후 commission: " + commissionAndTaxData.getCommission() + " // from: AskExecutionService.askExecuteProcess");
        System.out.println("======== 체결 후 tax: " + commissionAndTaxData.getTax() + " // from: AskExecutionService.askExecuteProcess");
        long fee = Math.addExact(commissionAndTaxData.getCommission(), commissionAndTaxData.getTax());
        Long total_filled_amount = Math.subtractExact(notionalValue, fee);

        // 계좌 입금
        memberAccount.deposit(total_filled_amount);
        log.info("전체 잔고 {}원 증감", total_filled_amount);

        /// 5. 주문 상태 변경
        orderLog.updateOrderStatus(OrderStatus.PARTIALLY_FILLED);
        orderLog.updateFilledAt();
        // 잔여 수량 감소
        long remainQuantity = orderLog.decRemainQuantity(executionEvent.getQuantity());
        if (remainQuantity == 0L) {
            orderLog.updateOrderStatus(OrderStatus.FILLED);
        } else if (orderLog.getOrderKind() == OrderKind.MARKET) {
            Long added = redisTemplate.opsForSet().add("order-id:" + orderLog.getId(), String.valueOf(remainQuantity));
            if (added != null && added == 0) {
                System.out.println("askExecuteProcess: 환불 로직 시작");
                stockHolding.increaseAvaQuantity(remainQuantity);
            }
        }

        /// 6. 원장 기록
        Ledger ledger = Ledger.builder()
                .orderLogId(orderLog.getId())
                .debitAccountId(memberAccount.getId())
                .creditAccountId(memberAccount.getBrokerageId())
                .ticker(executionEvent.getTicker())
                .debit(total_filled_amount)
                .credit(notionalValue)
                .qtyChange(executionEvent.getQuantity())
                .amountChange(executionEvent.getPrice())
                .commission(commissionAndTaxData.getCommission())
                .tax(commissionAndTaxData.getTax())
                .build();
        ledgerRepository.save(ledger);
        // TODO: 카프카 발행 및 원장 서비스 모듈 분리

        return true;
    }

    public void refundAvaQuantity(UUID memberAccountId, String ticker, Long remainQty) {
        StockHolding stockHolding = stockHoldingRepository
                .findByMemberAccountIdAndTicker(memberAccountId, ticker)
                .orElseThrow(() -> new EntityNotFoundException("해당 보유 주식이 존재하지 않습니다."));
        stockHolding.increaseAvaQuantity(remainQty);
    }

}
