package com.beyond.MKX.common.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginReqDto {
    private String email;    // 사용자가 입력한 이메일
    private String password; // 사용자가 입력한 비밀번호
}
