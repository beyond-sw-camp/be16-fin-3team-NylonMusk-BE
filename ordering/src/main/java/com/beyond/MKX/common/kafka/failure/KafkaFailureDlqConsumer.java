package com.beyond.MKX.common.kafka.failure;

import com.beyond.MKX.common.kafka.event.ExecutionEvent;
import com.beyond.MKX.common.kafka.event.OrderStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaFailureDlqConsumer {

    private final KafkaFailureLogService kafkaFailureLogService;

    @KafkaListener(
            topics = "executions.DLQ",
            groupId = "kafka-failure-log-group",
            containerFactory = "kafkaExecutionDlqListenerFactory"
    )
    public void consumeExecutionDlq(ConsumerRecord<String, ExecutionEvent> record, Acknowledgment ack) {
        kafkaFailureLogService.saveDlqRecord(record);
        ack.acknowledge();
        log.info("Execution DLQ 실패 이력 저장 완료: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
    }

    @KafkaListener(
            topics = "order-status.DLQ",
            groupId = "kafka-failure-log-group",
            containerFactory = "kafkaOrderStatusDlqListenerFactory"
    )
    public void consumeOrderStatusDlq(ConsumerRecord<String, OrderStatusEvent> record, Acknowledgment ack) {
        kafkaFailureLogService.saveDlqRecord(record);
        ack.acknowledge();
        log.info("OrderStatus DLQ 실패 이력 저장 완료: topic={}, partition={}, offset={}",
                record.topic(), record.partition(), record.offset());
    }
}
