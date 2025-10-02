package com.beyond.MKX.domain.admin.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
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
    private Status status;
}
