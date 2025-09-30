package com.beyond.MKX.common.loader;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.entity.Status;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InitialAdminLoader implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    // 관리자 계정 정보
    @Value("${init.admin.email}")
    private String adminEmail;

    @Value("${init.admin.password}")
    private String adminPassword;

    @Value("${init.admin.name}")
    private String adminName;

    @Value("${init.admin.phone}")
    private String adminPhone;

    @Override
    public void run(String... args) {
        // 조직 생성 or 조회

        // 관리자 계정 생성 or 조회
        adminRepository.findByEmail(adminEmail.toLowerCase())
                .orElseGet(() -> {
                    Admin admin = Admin.builder()
                            .email(adminEmail.toLowerCase())
                            .password(passwordEncoder.encode(adminPassword))
                            .name(adminName)
                            .phone(adminPhone)
                            .role(Role.EXCHANGE)
                            .status(Status.ACTIVE)
                            .build();
                    return adminRepository.save(admin);
                });
    }
}
