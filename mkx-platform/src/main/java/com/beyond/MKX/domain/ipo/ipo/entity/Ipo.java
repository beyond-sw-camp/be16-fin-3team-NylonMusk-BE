package com.beyond.MKX.domain.ipo.ipo.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ipo extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "corporation_id")
    private Corporation corporation;

    /* 심볼명 */
    @Column(length = 50, nullable = false, unique = true)
    private String symbol;

    /* 상장 요청 시각 */
    private LocalDateTime requestedAt;

    /* 상장 시각 */
    private LocalDateTime listingAt;

    /* 액면가(상장 주체가 주식을 처음 발행할 때 주권에 기재하는 1주당 가격) */
    private Long faceValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IpoStatus status; // REQUESTED, UNDER_REVIEW, APPROVED, LISTED, REJECTED, CANCELLED

    /* 상장일 기준가 */
    private Long priceOnListing;

    /* 상장 전 비상장주식 수 */
    @Column(nullable = false)
    private Long preIpoOutstandingShares;

    /* 상장 시점 총 발행 주식 수 = 비상장 주식 수 + 배정 및 정산 완료 후 (SETTLED 된 값)  */
    private Long outstandingSharesAtListing;

//    /* 대주주 지분율 */
//    @Column(nullable = false)
//    private Double majorShareholderRatio; // 0.0 ~ 1.0

    /* 보호예수 비율 */
    @Builder.Default
    private Double lockupRatio = 1.0; // 기본값: 서비스 계층에서 1.0 세팅!

    private String preShareholdersFileUrl;

    private String financialStatementsUrl;

    @Column(columnDefinition = "TEXT")
    private String rejectReason;

    /* 보호예수 해제일 */
    private LocalDateTime lockupExpiryAt; // 보통은 6개월이나 30분으로 프로젝트이므로 30분으로 설정할 예정

    /* 유통 가능 주식 수 */
    @Column(nullable = true)
    private Long floatSharesAtListing; //

    /* 공모 여부 */
    @Column(nullable = false)
    private Boolean isOffering;

    private LocalDateTime reviewedAt;
    private LocalDateTime listedAt;


//    // 상장일 스냅샷 유통 가능 주식 수 계산
//    public long calcFloatSharesAtListing() {
//        double lockedByMajor = totalShares * majorShareholderRatio * lockupRatio; // 대주주 지분 중 락업 비율만큼 비유통
//        long result = Math.round(totalShares - lockedByMajor);
//        return Math.max(result, 0L); // 방어적 처리
//    }

    public void applyLockupDefault() {
        // 대주주 지분 보호예수(락업) 100%
        this.lockupRatio = 1.0;
        // 상장이 되었을 때, 보호예수 해제는 30분 뒤에 이루어짐, 실제로는 6개월이라고 함
        if (listingAt != null) {
            this.lockupExpiryAt = listingAt.plusMinutes(30);
        }
    }

    public void approve() {
        if (this.status != IpoStatus.REQUESTED && this.status != IpoStatus.UNDER_REVIEW) {
            throw new IllegalStateException("승인 가능한 상태가 아닙니다.");
        }
        this.status = IpoStatus.APPROVED;
    }

    public void reject(String reason) {
        if (this.status != IpoStatus.REQUESTED && this.status != IpoStatus.UNDER_REVIEW) {
            throw new IllegalStateException("반려 가능한 상태가 아닙니다.");
        }
        this.rejectReason = reason;
        this.status = IpoStatus.REJECTED;
    }

    public void markUnderReview() { this.status = IpoStatus.UNDER_REVIEW; }

    public void list(LocalDateTime listingAt, Long priceOnListing) {
        if (this.status != IpoStatus.APPROVED) {
            throw new IllegalArgumentException("APPROVED 상태만 LISTED 전환 가능합니다.");
        }
        if (this.listingAt != null || this.status == IpoStatus.LISTED) {
            throw new IllegalArgumentException("이미 상장된 건입니다.");
        }
        if (priceOnListing == null || priceOnListing <= 0) {
            throw new IllegalArgumentException("상장 기준가(시초가)는 양수여야 합니다.");
        }

        this.listingAt = listingAt;
        this.priceOnListing = priceOnListing;
        applyLockupDefault();
        // 상장일 스냅샷 확정
//        this.floatSharesAtListing = calcFloatSharesAtListing();
        this.status = IpoStatus.LISTED;
    }

    public void updatePreShareholdersFileUrl(String url) {
        this.preShareholdersFileUrl = url;
    }

    public void updateFinancialStatementsUrl(String url) {
        this.financialStatementsUrl = url;
    }

    public void updateOutstandingSharesAtListing(Long total) {
        this.outstandingSharesAtListing = total;
    }



}
