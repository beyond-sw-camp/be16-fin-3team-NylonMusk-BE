package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.common.kafka.event.OrderStatusEvent;
import com.beyond.MKX.domain.order.entity.OrderKind;
import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.entity.Side;
import com.beyond.MKX.domain.order.repository.OrderLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class KafkaOrderStatusConsumer {

    private final OrderLogRepository orderLogRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final BidExecutionService bidExecutionService;
    private final AskExecutionService askExecutionService;

    public KafkaOrderStatusConsumer(OrderLogRepository orderLogRepository,
                                    @Qualifier("idempotency") RedisTemplate<String, String> redisTemplate, BidExecutionService bidExecutionService, AskExecutionService askExecutionService
    ) {
        this.orderLogRepository = orderLogRepository;
        this.redisTemplate = redisTemplate;
        this.bidExecutionService = bidExecutionService;
        this.askExecutionService = askExecutionService;
    }


    @KafkaListener(
            topics = "order-status",
            groupId = "${spring.kafka.consumer.order-status-group-id}",
            containerFactory = "kafkaOrderStatusListenerFactory"
    )
    public void processOrderStatus(
            @Payload OrderStatusEvent orderStatusEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment ack
    ) {
        try {
            System.out.println("=== partition = " + partition + "  offset = " + offset + " OrderStatus 이벤트 컨슘 시작 ===");
            System.out.println("orderStatusEvent = " + orderStatusEvent);

            OrderLog orderLog = orderLogRepository.findById(UUID.fromString(orderStatusEvent.getOrderId()))
                    .orElseThrow(() -> new EntityNotFoundException("해당 주문기록을 찾을 수 없습니다."));

            String response = switch (orderStatusEvent.getStatus()) {
                case "MARKET_PARTIAL"-> handleMarketOrderRefund(orderLog, orderStatusEvent);
                case "CANCEL_OK"-> handleCanceledOrder();
                default-> null;
            };
            if (response != null) {
                log.info("{}", response);
            }

            ack.acknowledge(); // 수동 커밋
            System.out.println("=========== OrderStatus 이벤트 컨슘 종료 및 커밋 ===========");
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
            throw e;
        }

    }


    // 보유 수량 조회 및 환불 로직 처리
    private String handleMarketOrderRefund(OrderLog orderLog, OrderStatusEvent orderStatusEvent) {
        Long remainQty = orderStatusEvent.getRemaining() == null ? 0L : orderStatusEvent.getRemaining().longValueExact();
        if (orderLog.getOrderKind() == OrderKind.MARKET && remainQty != 0L) {
            /**
             * 레디스에 잔여 수량 적재로 인한 시장가 환불 처리 위치 판단
             * 1: 적재 성공 -> 추후 executionConsumer 에서 환불 로직 처리
             * 0: 적재 실패 -> 이미 레디스에 있기에 orderStatusConsumer 에서 환불 로직 처리
             */
            Long added = redisTemplate.opsForSet().add("order-id:" + orderLog.getId(), String.valueOf(remainQty));
            if (added != null && added == 0) {
                if (orderLog.getSide() == Side.BUY) {
                    bidExecutionService.refundFreezeAmount(orderLog.getId());
                    return "KafkaOrderStatusConsumer: 매수 환불 로직 완료";
                } else {
                    askExecutionService.refundAvaQuantity(
                            orderLog.getAccount().getMemberId(),
                            orderStatusEvent.getTicker(),
                            remainQty
                    );
                    return "KafkaOrderStatusConsumer: 매도 환불 로직 완료";
                }
            }
            return "KafkaOrderStatusConsumer: 시장가 멱등 수량 적재 완료";
        }

        return null;
    }

    // 주문 취소 후처리
    private String handleCanceledOrder() {
        System.out.println("KafkaOrderStatusConsumer.handleCanceledOrder");
        return null;
    }


}
