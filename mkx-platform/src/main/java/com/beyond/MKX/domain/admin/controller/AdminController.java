package com.beyond.MKX.domain.admin.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.domain.admin.dto.AdminResDto;
import com.beyond.MKX.domain.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController  {
    private final AdminService adminService;

    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@AuthenticationPrincipal CustomAdminPrincipal principal) {
        AdminResDto dto = adminService.getMeWithOrgsAndStocks(principal.id());
        return ApiResponse.ok(dto, "내 정보 조회 성공");
    }
}
