package com.beyond.MKX.domain.outbox.service;

import com.beyond.MKX.domain.outbox.entity.OrderOutbox;
import com.beyond.MKX.domain.outbox.repository.OrderOutboxRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OutBoxService {

    private final OrderOutboxRepository outboxRepository;


    public List<OrderOutbox> findUnpublishedBatch(int batchSize) {
        List<OrderOutbox> outboxList = outboxRepository.findUnpublishedBatch(PageRequest.of(0, batchSize));

        for (OrderOutbox orderOutbox : outboxList) {
            orderOutbox.markAsPublished();
        }

        return outboxList;
    }

    public void revertToUnpublished(UUID outboxId) {
        OrderOutbox managedOutbox = outboxRepository.findById(outboxId)
                .orElseThrow(() -> new EntityNotFoundException("Outbox not found: " + outboxId));

        managedOutbox.revertToUnpublished();
    }

}
