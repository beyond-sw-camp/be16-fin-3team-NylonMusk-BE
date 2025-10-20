package com.beyond.MKX.domain.assets.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "stock_update_event", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_update_event_key", columnNames = "idempotency_key")
})
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockUpdateEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private UUID idempotencyKey;

    @Column(nullable = false)
    private LocalDateTime processedAt;
}
