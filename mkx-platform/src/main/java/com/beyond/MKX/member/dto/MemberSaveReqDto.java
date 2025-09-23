package com.beyond.MKX.member.dto;

import com.beyond.MKX.member.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MemberSaveReqDto {
    private String name;
    private String email;
    private String password;

    public Member toEntity(String encodedPassword){
        return Member.builder()
                .name(this.name)
                .email(this.email)
                .password(encodedPassword)
                .build();
    }
}
