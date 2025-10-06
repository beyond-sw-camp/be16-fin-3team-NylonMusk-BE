package com.beyond.MKX.domain.order.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderOutboxRepository extends JpaRepository<OrderOutbox, UUID> {
}
