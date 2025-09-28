package com.beyond.MKX.domain.organization.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기업 엔터티 (companies 테이블 매핑)
 *
 *  설계 포인트
 * - 공통 PK/시간 필드는 BaseIdAndTimeEntity에서 상속 (id, createdAt, updatedAt 등)
 * - Setter 금지(불변에 가깝게): 생성 후 변경은 서비스/도메인 메서드로만 제어(추후 필요 시 추가)
 * - ENUM은 STRING으로 저장하여 순서(ordinal) 변경에 따른 장애를 방지
 *
 *  용도
 * - 상장 기업(ISSUER), 브로커(BROKER), 법인 거래자(TRADER) 등 "회사" 단위의 주체 식별
 * - Member와 1:N 관계(여러 멤버가 한 회사에 소속)
 */
@Entity
@Table(name = "organization")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organization extends BaseIdAndTimeEntity {


    // 회사명(한글)
    @Column(name = "name_ko", nullable = false)
    private String nameKo;

    //회사명(영문)
    @Column(name = "name_eng")
    private String nameEng;

    // 법인등록번호
    @Column(name = "reg_no", nullable = false, unique = true)
    private String regNo;

    // 회사 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "company_type", nullable = false)
    private OrganizationType companyType;

    /**
     * 회사 상태
     * - ACTIVE   : 정상
     * - PENDING  : 승인 대기(심사 중 등)
     * - DELISTED : 상장폐지/비활성
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "company_status", nullable = false)
    private OrganizationStatus companyStatus;

    // 업종
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    // 설립일
    @Column(name = "established_date")
    private String establishedDate;

    // 도로명주소
    @Column(name = "road_address")
    private String roadAddress;

    // 상세 주소
    @Column(name = "detail_address")
    private String detailAddress;

    // 대표자명
    @Column(name = "owner_name")
    private String ownerName;
}