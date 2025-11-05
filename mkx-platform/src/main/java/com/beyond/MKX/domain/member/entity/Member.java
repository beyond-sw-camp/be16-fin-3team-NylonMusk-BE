package com.beyond.MKX.domain.member.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "member")
public class Member extends BaseIdAndTimeEntity {


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brokerage_id", nullable = false)
    private SecuritiesFirm brokerage;

    @Column(nullable = false, length = 10)
    private String name;

    @Column(nullable = false, length = 50, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 15, unique = true)
    private String phone;

    /** 생년월일 (신분증 OCR로 추출, 선택적) */
    @Column(nullable = true)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private MemberStatus status = MemberStatus.ACTIVE;

    /** EMAIL_VERIFICATION: 이메일 인증 토큰 */
    @Column(nullable = true, length = 255)
    private String emailVerificationToken;

    /** EMAIL_VERIFICATION: 이메일 인증 완료 시간 */
    @Column(nullable = true)
    private LocalDateTime emailVerifiedAt;

    /** EMAIL_VERIFICATION: 이메일 인증 토큰 만료 시간 */
    @Column(nullable = true)
    private LocalDateTime emailVerificationTokenExpiresAt;

}
