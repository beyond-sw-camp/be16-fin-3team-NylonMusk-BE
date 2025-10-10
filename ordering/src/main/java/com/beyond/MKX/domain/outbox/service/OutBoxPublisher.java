package com.beyond.MKX.domain.outbox.service;

import com.beyond.MKX.domain.outbox.entity.OrderOutbox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class OutBoxPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OutBoxService outBoxService;

    public OutBoxPublisher(KafkaTemplate<String, Object> kafkaTemplate, OutBoxService outBoxService) {
        this.kafkaTemplate = kafkaTemplate;
        this.outBoxService = outBoxService;
    }

    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Scheduled(fixedDelay = 30000) // 실서비스는 5ms로 설정
    public void publishOrder() {
        // 데이터 조회(SKIP LOCKED)
        List<OrderOutbox> batch = outBoxService.findUnpublishedBatch(200);

        // 카프카 발행
        for (OrderOutbox orderOutbox : batch) {
            try {
                String payload = orderOutbox.getPayload();
                kafkaTemplate.send("place-order", orderOutbox.getKafkaKey(), payload);
                log.info("주문 OutBox 발행 성공: {}", payload);
            } catch (Exception e) {
                log.error("{}주문 OutBox 발행 실패", orderOutbox.getId());
                outBoxService.revertToUnpublished(orderOutbox.getId());
            }
        }
    }

}
