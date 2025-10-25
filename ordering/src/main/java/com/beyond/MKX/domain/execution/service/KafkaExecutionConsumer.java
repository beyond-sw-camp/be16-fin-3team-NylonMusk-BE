package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.domain.order.entity.Side;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaExecutionConsumer {

    private final BidExecutionService bidExecutionService;
    private final AskExecutionService askExecutionService;

    @KafkaListener(
            topics = "executions",
            groupId = "${spring.kafka.consumer.execution-group-id}",
            containerFactory = "kafkaExecutionListenerFactory"
    )
    public void processExecute(
            @Payload ExecutionEvent executionEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) String partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment ack
    ) {
        try {
            System.out.println("=== partition = " + partition + "  offset = " + offset + " 체결 후처리 컨슘 시작 ===");
            System.out.println("executionEvent = " + executionEvent);

            // 0. 멱등성 검사

            // 1. 매수자와 매도자 구분
            UUID bidOrderId = null;
            UUID askOrderId = null;
            if (executionEvent.getSide() == Side.BUY) {
                bidOrderId = UUID.fromString(executionEvent.getMarketOrderId());
                askOrderId = UUID.fromString(executionEvent.getCounterOrderId());
            } else if (executionEvent.getSide() == Side.SELL) {
                askOrderId = UUID.fromString(executionEvent.getMarketOrderId());
                bidOrderId = UUID.fromString(executionEvent.getCounterOrderId());
            }

            // 2. 매수자 체결 로직
            boolean isFilledBid = bidExecutionService.bidExecuteProcess(bidOrderId, executionEvent);
            if (!isFilledBid) {
                log.info("이미 처리된 매수자 체결 로직");
            }

            // 3. 매도자 체결 로직
            boolean isFilledAsk = askExecutionService.askExecuteProcess(askOrderId, executionEvent);
            if (!isFilledAsk) {
                log.info("이미 처리된 매도자 체결 로직");
            }

            // 4. 카프카 수동 커밋
            ack.acknowledge(); // 수동 커밋
            System.out.println("=========== 체결 후처리 컨슘 종료 및 커밋 ===========");
        } catch (Exception e) {
            log.error("Exception: {}", e.getMessage());
            throw e;
        }

    }
}
