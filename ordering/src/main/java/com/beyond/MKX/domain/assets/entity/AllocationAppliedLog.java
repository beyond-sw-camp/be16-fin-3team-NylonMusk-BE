package com.beyond.MKX.domain.assets.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name="allocation_applied_log",
        uniqueConstraints=@UniqueConstraint(name="uk_alloc_event", columnNames="allocation_event_id"))
@Getter @NoArgsConstructor @AllArgsConstructor @Builder
public class AllocationAppliedLog extends BaseIdAndTimeEntity {
    @Column(name="allocation_event_id", nullable=false)
    private UUID allocationEventId;
}
