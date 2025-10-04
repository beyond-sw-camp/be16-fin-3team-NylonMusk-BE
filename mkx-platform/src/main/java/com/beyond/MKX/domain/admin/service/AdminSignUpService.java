package com.beyond.MKX.domain.admin.service;

import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.entity.Status;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminSignUpService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public Admin createPendingAdmin(String email,
                                    String phone,
                                    String name,
                                    String rawPassword,
                                    Role role,
                                    Corporation corporation,
                                    SecuritiesFirm securitiesFirm) {

        validateDuplicate(email, phone);

        Admin admin = Admin.builder()
                .email(email.toLowerCase())
                .phone(phone)
                .name(name)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .status(Status.PENDING)
                .corporation(corporation)
                .securitiesFirm(securitiesFirm)
                .build();

        return adminRepository.save(admin);
    }

    // 이메일 전화번호 검증 로직
    private void validateDuplicate(String email, String phone) {
        if (adminRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("이미 등록된 관리자 이메일입니다.");
        }

        if (adminRepository.existsByPhone(phone)) {
            throw new DuplicateResourceException("이미 등록된 관리자 전화번호입니다.");
        }
    }
}
