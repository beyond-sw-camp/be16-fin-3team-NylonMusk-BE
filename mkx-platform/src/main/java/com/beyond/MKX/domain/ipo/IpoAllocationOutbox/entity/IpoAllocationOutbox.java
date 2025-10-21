package com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity;// package com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.ipo.IpoAllocationOutbox.entity.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "ipo_allocation_outbox",
        uniqueConstraints = @UniqueConstraint(name="uk_outbox_idem", columnNames = "idempotency_key")
)
public class IpoAllocationOutbox extends BaseIdAndTimeEntity {

    @Column(name = "idempotency_key", nullable = false)
    private UUID idempotencyKey;   // = allocationId

    @Column(name = "ipo_id", nullable = false)
    private UUID ipoId;

    @Column(name = "offering_id", nullable = false)
    private UUID offeringId;

    @Column(name = "allocation_id", nullable = false)
    private UUID allocationId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "brokerage_id")
    private UUID brokerageId;

    @Column(name = "qty", nullable = false)
    private long qty;

    @Column(name = "offer_price_snapshot")
    private Long offerPriceSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxStatus status;   // PENDING, SENT, FAILED

    public void markSent()   { this.status = OutboxStatus.SENT; }
    public void markFailed() { this.status = OutboxStatus.FAILED; }
}
