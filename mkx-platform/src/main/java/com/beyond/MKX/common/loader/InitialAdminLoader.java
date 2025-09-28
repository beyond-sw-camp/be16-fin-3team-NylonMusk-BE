package com.beyond.MKX.common.loader;

import com.beyond.MKX.domain.organization.entity.Organization;
import com.beyond.MKX.domain.organization.entity.OrganizationStatus;
import com.beyond.MKX.domain.organization.entity.OrganizationType;
import com.beyond.MKX.domain.organization.entity.Category;
import com.beyond.MKX.domain.organization.repository.OrganizationRepository;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.member.entity.Role;
import com.beyond.MKX.domain.member.entity.UserStatus;
import com.beyond.MKX.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class InitialAdminLoader implements CommandLineRunner {

    private final OrganizationRepository organizationRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회사 정보
    @Value("${init.admin.company.nameKo}")
    private String companyNameKo;

    @Value("${init.admin.company.nameEng}")
    private String companyNameEng;

    @Value("${init.admin.company.regNo}")
    private String companyRegNo;

    @Value("${init.admin.company.companyType}")
    private String companyType;

    @Value("${init.admin.company.status}")
    private String companyStatus;

    @Value("${init.admin.company.category}")
    private String companyCategory;

    @Value("${init.admin.company.establishedDate}")
    private String companyEstablishedDate;

    @Value("${init.admin.company.roadAddress}")
    private String companyRoadAddress;

    @Value("${init.admin.company.detailAddress}")
    private String companyDetailAddress;

    @Value("${init.admin.company.ownerName}")
    private String companyOwnerName;

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
        Organization exchange = organizationRepository.findByRegNo(companyRegNo.replaceAll("\\D", ""))
                .orElseGet(() -> {
                    Organization newOrg = Organization.builder()
                            .nameKo(companyNameKo)
                            .nameEng(companyNameEng)
                            .regNo(companyRegNo.replaceAll("\\D", ""))
                            .companyType(OrganizationType.valueOf(companyType.toUpperCase()))
                            .companyStatus(OrganizationStatus.valueOf(companyStatus.toUpperCase()))
                            .category(Category.valueOf(companyCategory.toUpperCase()))
                            .establishedDate(companyEstablishedDate)
                            .roadAddress(companyRoadAddress)
                            .detailAddress(companyDetailAddress)
                            .ownerName(companyOwnerName)
                            .build();
                    return organizationRepository.save(newOrg);
                });

        // 관리자 계정 생성 or 조회
        memberRepository.findByEmail(adminEmail.toLowerCase())
                .orElseGet(() -> {
                    Member admin = Member.builder()
                            .organization(exchange)
                            .email(adminEmail.toLowerCase())
                            .passwordHash(passwordEncoder.encode(adminPassword))
                            .name(adminName)
                            .phone(adminPhone)
                            .role(Role.ADMIN)
                            .userStatus(UserStatus.ACTIVE)
                            .position("ADMIN") // 필수 컬럼 매핑
                            .build();
                    return memberRepository.save(admin);
                });
    }
}
