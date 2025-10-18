package com.beyond.MKX.domain.disclosure.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Disclosure (공시)
 * - 거래소에 제출된 종목 단위 공시 문서
 * - PDF 등 파일 기반
 * - 기업 단위가 아닌 Stock(상장 종목) 기준으로 관리
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Entity
@Table(name = "disclosure")
public class Disclosure extends BaseIdAndTimeEntity {

    @Comment("대상 종목 ID (상장된 주식)")
    @Column(nullable = false)
    private UUID stockId;

    @Comment("공시 유형 (예: 재무제표, 주요사항, 정정공시 등)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DisclosureType disclosureType;

    @Comment("공시 제목")
    @Column(nullable = false, length = 100)
    private String title;

    @Comment("요약 설명")
    @Column(length = 300)
    private String summary;

    @Comment("첨부 파일 경로 (PDF 등)")
    @Column(nullable = false, length = 512)
    private String fileUrl;

    @Comment("공시 상태 (대기/승인/반려)")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisclosureStatus status;

    @Comment("공시 공개 시각 (승인 시 자동 세팅)")
    private LocalDateTime publishedAt;

    @Comment("공시 당시 종목명 (스냅샷)")
    @Column(nullable = false, length = 100)
    private String stockNameSnapshot;

    @Comment("공시 당시 티커 (스냅샷)")
    @Column(nullable = false, length = 20)
    private String tickerSnapshot;

    // ===== 비즈니스 로직 =====

    public void approve() {
        this.status = DisclosureStatus.APPROVED;
        this.publishedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = DisclosureStatus.REJECTED;
    }

    public void updateFile(String newUrl, String newSummary) {
        if (newUrl != null && !newUrl.isBlank()) this.fileUrl = newUrl;
        if (newSummary != null) this.summary = newSummary;
        this.status = DisclosureStatus.PENDING; // 수정 시 재심사 대기
    }
}
