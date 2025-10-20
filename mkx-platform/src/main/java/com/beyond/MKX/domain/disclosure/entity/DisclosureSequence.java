package com.beyond.MKX.domain.disclosure.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "disclosure_sequence",
        uniqueConstraints = @UniqueConstraint(name = "uk_disclosure_seq_year", columnNames = "year"))
public class DisclosureSequence extends BaseIdAndTimeEntity {

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "current_no", nullable = false)
    private int currentNo;

    public int nextAndGet() {
        this.currentNo = this.currentNo + 1;
        return this.currentNo;
    }
}

