package com.beyond.MKX.domain.disclosure.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "disclosure_decision",
        indexes = {
                @Index(name = "ix_decision_disclosure_id", columnList = "disclosure_id"),
                @Index(name = "ix_decision_action_created", columnList = "action,created_at"),
                @Index(name = "ix_decision_reject_code", columnList = "reject_code")
        })
public class DisclosureDecision extends BaseIdAndTimeEntity {

    @Column(name = "disclosure_id", nullable = false)
    private UUID disclosureId;

    @Column(name = "admin_id", nullable = false)
    private UUID adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private DisclosureDecisionAction action; // APPROVE or REJECT

    @Enumerated(EnumType.STRING)
    @Column(name = "reject_code", length = 30)
    private DisclosureRejectCode rejectCode; // REJECT일 때 선택

    @Column(name = "reason", length = 255)
    private String reason; // REJECT 상세 사유(선택)
}

