package com.beyond.MKX.domain.securities_firm.entity;



import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "securities_firm")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecuritiesFirm extends BaseIdAndTimeEntity {

    // 증권사명 국문
    @Column(nullable = false, length = 50)
    private String nameKo;

    // 증권사명 영문
    @Column(nullable = false, length = 100)
    private String nameEng;

    // 대표자명
    @Column(nullable = false, length = 10)
    private String ownerName;

    // 사업자등록번호
    @Column(nullable = false, length = 30, unique = true)
    private String regNo;

    // 증권사 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING; // PENDING, ACTIVE, REJECTED, DELISTED

    // 설립일
    @Column(nullable = false)
    private LocalDate establishedDate;

    // 도로명주소
    @Column(nullable = false, length = 50)
    private String roadAddress;

    // 상세주소
    @Column(nullable = false, length = 50)
    private String detailAddress;

    // 금융투자업 인가번호
    @Column(nullable = false, length = 50)
    private String financialInvestmentLicenseNo;

    // 금융투자업 인가증 url
    @Column(nullable = false, length = 512)
    private String financialInvestmentLicenseDoc;

    // 사업자등록증 url
    @Column(nullable = false, length = 512)
    private String businessRegistrationCert;

    // 법인인감증명서 url
    @Column(nullable = false, length = 512)
    private String corporateSealCert;

    // 거래소 수수료율
    @Column(nullable = false)
    private Double exchangeFee;

    // 가입 거절 사유
    @Column(length = 255)
    private String rejectReason;

    // 소프트 딜리트 시각 (null이면 활성)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Status {
        PENDING, ACTIVE, REJECTED, DELISTED
    }

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
