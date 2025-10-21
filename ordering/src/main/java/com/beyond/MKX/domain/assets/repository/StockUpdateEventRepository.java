package com.beyond.MKX.domain.assets.repository;

import com.beyond.MKX.domain.assets.entity.StockUpdateEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockUpdateEventRepository extends JpaRepository<StockUpdateEvent, UUID> {
    boolean existsByIdempotencyKey(UUID idempotencyKey);
}
