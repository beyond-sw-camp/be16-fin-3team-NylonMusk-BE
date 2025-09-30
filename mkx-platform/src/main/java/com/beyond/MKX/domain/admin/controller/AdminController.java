package com.beyond.MKX.domain.admin.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.admin.dto.AdminResDto;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.service.AdminService;
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
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController  {
    private final AdminService adminService;

    // Gateway에서 붙여준 X-User-Id → UUID 로 변환해서 사용
    @GetMapping("/me")
    public ResponseEntity<?> getMyInfo(@RequestHeader("X-User-Id") String userId) {
        UUID adminId;
        try {
            adminId = UUID.fromString(userId);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 사용자 ID 형식");
        }

        Admin admin = adminService.getMemberById(adminId);
        AdminResDto dto = AdminResDto.from(admin);

        return ApiResponse.ok(dto, "내 정보 조회 성공");
    }
}
