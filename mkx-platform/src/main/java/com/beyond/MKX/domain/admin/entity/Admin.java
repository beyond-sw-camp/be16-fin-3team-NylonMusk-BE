package com.beyond.MKX.domain.admin.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Admin extends BaseIdAndTimeEntity {


    // 관리자 이름
    @Column(name = "name", nullable = false, length = 10)
    private String name;

    // 전화번호
    @Column(name = "phone", nullable = false, unique = true, length = 20)
    private String phone;

    // 관리자 이메일
    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    // 관리자 비밀번호
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    // 관리자 역할
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    // 관리자 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status.PENDING;

    // 소속 법인
    @ManyToOne
    @JoinColumn(name = "corporation_id")
    private Corporation corporation;

    // 소속 증권사
    @ManyToOne
    @JoinColumn(name = "securities_firm_id")
    private SecuritiesFirm securitiesFirm;

    public void changeStatus(Status status) {
        this.status = status;
    }
}
