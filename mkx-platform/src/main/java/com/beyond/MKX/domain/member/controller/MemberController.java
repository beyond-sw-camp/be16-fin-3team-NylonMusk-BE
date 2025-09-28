package com.beyond.MKX.domain.member.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.member.dto.MemberResDto;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import com.beyond.MKX.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // Gateway에서 붙여준 X-User-Id → UUID 로 변환해서 사용
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@RequestHeader("X-User-Id") String userId) {
        UUID memberId;
        try {
            memberId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 사용자 ID 형식");
        }

        Member member = memberService.getMemberById(memberId);
        MemberResDto dto = MemberResDto.from(member);

        return ApiResponse.ok(dto, "내 정보 조회 성공");
    }
}