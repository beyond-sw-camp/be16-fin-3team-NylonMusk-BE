package com.beyond.MKX.domain.ipo.bookbuilding.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "ipo_book_building")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpoBookBuilding extends BaseIdAndTimeEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ipo_offering_id", nullable = false)
    private IpoOffering ipoOffering;

    /** 참여 주체 유형: CORPORATION, BROKERAGE, EXCHANGE */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantType participantType;

    @Column(nullable = false)
    private UUID participantId;

    /** 희망가격 (공모가 밴드 내, null 시 모든 가격 참여로 간주) */
    @Column(nullable = true)
    private Long bidPrice; // 희망가격

    @Column(nullable = false)
    private Long bidQuantity; // 희망수량

    /** 모든 가격 구간 참여 여부 */
    @Column(nullable = false)
    @Builder.Default
    private Boolean acceptAllPrices = false;

    @Builder.Default
    private Boolean alreadyParticipated = false;

}
