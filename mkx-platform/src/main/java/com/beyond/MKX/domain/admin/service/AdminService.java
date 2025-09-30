package com.beyond.MKX.domain.admin.service;

import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    /**
     * userId에 해당하는 Member 조회
     * - 없는 경우 null 반환 (Controller에서 처리)
     * - 추후 권한/상태 체크 같은 비즈니스 로직 추가 가능
     */
    public Admin getMemberById(UUID userId) {
        return adminRepository.findById(userId).orElseThrow(() ->
                new DuplicateResourceException("회원 정보를 찾을 수 없습니다."));
    }
}