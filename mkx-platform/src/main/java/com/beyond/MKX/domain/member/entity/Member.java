package com.beyond.MKX.domain.member.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.organization.entity.Organization;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원 엔터티 (member 테이블 매핑)
 *
 *  설계 포인트
 * - 회사(Company)와 다대일 관계: member.company_id → company.id (FK)
 * - 이메일/전화번호는 UNIQUE. 표준화(lowercase/하이픈 제거 등)는 서비스 계층에서 처리 권장
 * - role/user_status는 Enum을 STRING으로 저장
 *
 *  용도
 * - 거래소에 로그인하는 계정(관리자/기업/브로커)
 * - 인증/인가의 주체(권한 부여, 화면 접근 통제)
 */
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor   // JPA용
@AllArgsConstructor  // 테스트/시드 데이터 편의
@Builder
public class Member extends BaseIdAndTimeEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * 로그인 ID로 사용되는 이메일
     * NOT NULL + UNIQUE
     * - 보안/일관성을 위해 저장 전 소문자 변환 권장
     * - 개인정보 보호를 위해 로그/예외 메시지에 직접 노출 금지
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;


    // 비밀번호
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // 이름
    @Column(name = "name", nullable = false)
    private String name;

   // 전화번호
    @Column(name = "phone", nullable = false, unique = true)
    private String phone;

    /**
     * 역할(Role)
     * - ADMIN      : 거래소 관리자
     * - CORPORATION  : 상장사 대표/관리자
     * - BROKERAGE   : 증권사 관리자
     * - BROKER  : 증권사 딜러(트레이더)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    /**
     * 계정 상태
     * - ACTIVE    : 정상
     * - SUSPENDED : 일시 정지(5회 실패 락 등)
     * - DELETED   : 삭제/탈퇴
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    private UserStatus userStatus;

    // 직급, 직책
    @Column(nullable = false)
    private String position;
}