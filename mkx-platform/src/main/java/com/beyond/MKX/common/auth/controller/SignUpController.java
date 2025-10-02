package com.beyond.MKX.common.auth.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.corporation.dto.CorporationSignUpReqDto;
import com.beyond.MKX.domain.corporation.service.CorporationService;
import com.beyond.MKX.domain.securities_firm.dto.SecuritiesFirmSignUpReqDto;
import com.beyond.MKX.domain.securities_firm.service.SecuritiesFirmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/signup")
@RequiredArgsConstructor
public class SignUpController {

    private final CorporationService corporationService;
    private final SecuritiesFirmService securitiesFirmService;

    // 기업 회원가입
    @PostMapping("/corporation")
    public ResponseEntity<?> corporationSignUp(@Valid @RequestBody CorporationSignUpReqDto request) {
        Admin admin = corporationService.signUpAdmin(request);
        return ApiResponse.created(admin.getId(), "기업 관리자 가입 신청 완료");
    }

    // 증권사 회원가입
    @PostMapping("/securities-firm")
    public ResponseEntity<?> securitiesFirmSignUp(@Valid @RequestBody SecuritiesFirmSignUpReqDto request) {
        Admin admin = securitiesFirmService.signUpAdmin(request);
        return ApiResponse.created(admin.getId(), "증권사 관리자 가입 신청 완료");
    }
}
