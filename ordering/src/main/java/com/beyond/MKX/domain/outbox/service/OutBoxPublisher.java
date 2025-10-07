package com.beyond.MKX.domain.order.outbox;

import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
public class OutBoxPublisher {

    private final OrderOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutBoxPublisher(OrderOutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

//    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Scheduled(fixedDelay = 200)
    public void publishOrder() {
        // 데이터 조회 -> skip locked
        List<OrderOutbox> batch = outboxRepository.findUnpublishedBatch(PageRequest.of(0, 200));

        // 카프카 발행
        for (OrderOutbox orderOutbox : batch) {
            String payload = orderOutbox.getPayload();
            kafkaTemplate.send("place-order", "test-삼성전자-005930", payload);

            // 주문 발행 완료 기록
            orderOutbox.markAsPublished();
            outboxRepository.save(orderOutbox);
        }
    }



}
