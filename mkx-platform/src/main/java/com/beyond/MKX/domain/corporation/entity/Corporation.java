package com.beyond.MKX.domain.corporation.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "corporation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Corporation extends BaseIdAndTimeEntity {

    // 기업명 국문
    @Column(nullable = false, length = 50)
    private String nameKo;

    // 기업명 영문
    @Column(nullable = false, length = 100)
    private String nameEng;

    // 대표자명
    @Column(nullable = false, length = 10)
    private String ownerName;

    // 사업자등록번호
    @Column(nullable = false, length = 30, unique = true)
    private String regNo;

    // 기업 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING; // PENDING, ACTIVE, REJECTED, DELISTED

    // 설립일
    @Column(nullable = false)
    private LocalDate estDate;

    // 도로명주소
    @Column(nullable = false, length = 50)
    private String roadAddress;

    // 상세주소
    @Column(nullable = false, length = 50)
    private String detailAddress;

    // 자본금
    @Column(nullable = false)
    private Long capital;

    // 최근 연매출
    @Column(nullable = false)
    private Long recentAnnualSales;

    // 사업자등록증 url
    @Column(nullable = false, length = 512)
    private String businessRegistrationCert;

    // 법인인감증명서 url
    @Column(nullable = false, length = 512)
    private String corporateSealCert;

    // 기업 로고 url
    @Column(name = "logo_url", nullable = false, length = 512)
    private String logoUrl;

    // 가입 거절 사유
    @Column(length = 255)
    private String rejectReason;

    // 소프트 딜리트 시각 (null이면 활성)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void approve() {
        this.status = Status.ACTIVE;
        this.rejectReason = null;
    }

    public void reject(String reason) {
        this.status = Status.REJECTED;
        this.rejectReason = reason;
    }

    public void softDelete(LocalDateTime when) {
        this.deletedAt = when;
    }
}
