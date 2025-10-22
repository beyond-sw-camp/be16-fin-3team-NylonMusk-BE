package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.OrderStatusEvent;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import com.beyond.MKX.domain.order.entity.OrderKind;
import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.entity.Side;
import com.beyond.MKX.domain.order.repository.OrderLogRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class RefundOrderService {

    private final OrderLogRepository orderLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final StockHoldingRepository stockHoldingRepository;

    public RefundOrderService(OrderLogRepository orderLogRepository,
                              @Qualifier("idempotency") RedisTemplate<String, String> redisTemplate,
                              StockHoldingRepository stockHoldingRepository
    ) {
        this.orderLogRepository = orderLogRepository;
        this.redisTemplate = redisTemplate;
        this.stockHoldingRepository = stockHoldingRepository;
    }


    // 보유 수량 조회 및 환불 로직 처리
    public String handleMarketOrderRefund(Long remainQty, OrderStatusEvent orderStatusEvent) {
        OrderLog orderLog = orderLogRepository.findById(UUID.fromString(orderStatusEvent.getOrderId()))
                .orElseThrow(() -> new EntityNotFoundException("해당 주문기록을 찾을 수 없습니다."));
        if (orderLog.getOrderKind() == OrderKind.MARKET && remainQty != 0L) {
            /**
             * 레디스에 잔여 수량 적재로 인한 시장가 환불 처리 위치 판단
             * 1: 적재 성공 -> 추후 executionConsumer 에서 환불 로직 처리
             * 0: 적재 실패 -> 이미 레디스에 있기에 orderStatusConsumer 에서 환불 로직 처리
             */
            Long added = redisTemplate.opsForSet().add("order-id:" + orderLog.getId(), String.valueOf(remainQty));
            if (added != null && added == 0) {
                if (orderLog.getSide() == Side.BUY) {
                    refundFreezeAmount(orderLog);
                    return "매수 환불 로직 완료";
                } else {
                    refundAvaQuantity(
                            orderLog.getAccount().getMemberId(),
                            orderStatusEvent.getTicker(),
                            remainQty
                    );
                    return "매도 환불 로직 완료";
                }
            }
            return "시장가 멱등 수량 적재 완료";
        }

        return "시장가 환불로직이 아닙니다.";
    }

    public String handleCanceledOrder() {

        return "handleCanceledOrder.ok";
    }


    /// **-------------- 내부 메서드들 --------------**

    private void refundFreezeAmount(OrderLog orderLog) {
        orderLog.getAccount().increaseAvailableBalance(orderLog.getFreezeAmount());
    }

    private void refundAvaQuantity(UUID memberAccountId, String ticker, Long remainQty) {
        StockHolding stockHolding = stockHoldingRepository
                .findByMemberAccountIdAndTicker(memberAccountId, ticker)
                .orElseThrow(() -> new EntityNotFoundException("해당 보유 주식이 존재하지 않습니다."));
        stockHolding.increaseAvaQuantity(remainQty);
    }


}
