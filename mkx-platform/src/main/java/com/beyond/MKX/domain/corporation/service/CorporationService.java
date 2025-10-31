package com.beyond.MKX.domain.corporation.service;

import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.service.AdminSignUpService;
import com.beyond.MKX.domain.corporation.dto.CorporationSignUpReqDto;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.entity.Status;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class CorporationService {

    private final CorporationRepository corporationRepository;
    private final AdminSignUpService adminSignUpService;
    private final S3Manager s3Manager;


    public Admin signUpAdmin(CorporationSignUpReqDto request) {
        validateDuplicateRegNo(request.getRegNo());

        String businessRegistrationCertUrl = uploadOrThrow(request.getBusinessRegistrationCertFile(),
                "corporations/business-registration");

        String corporateSealCertUrl = uploadOrThrow(request.getCorporateSealCertFile(),
                "corporations/corporate-seal");

        String logoUrl = uploadOrThrow(request.getLogoFile(),
                "corporations/logo");

        Corporation corporation = Corporation.builder()
                .nameKo(request.getNameKo())
                .nameEng(request.getNameEng())
                .ownerName(request.getOwnerName())
                .regNo(request.getRegNo())
                .status(Status.PENDING)
                .estDate(request.getEstDate())
                .roadAddress(request.getRoadAddress())
                .detailAddress(request.getDetailAddress())
                .capital(request.getCapital())
                .recentAnnualSales(request.getRecentAnnualSales())
                .businessRegistrationCert(businessRegistrationCertUrl)
                .corporateSealCert(corporateSealCertUrl)
                .logoUrl(logoUrl)
                .build();

        Corporation savedCorporation = corporationRepository.save(corporation);

        return adminSignUpService.createPendingAdmin(
                request.getAdminEmail(),
                request.getAdminPhone(),
                request.getAdminName(),
                request.getAdminPassword(),
                Role.CORPORATION,
                savedCorporation,
                null
        );
    }

    private void validateDuplicateRegNo(String regNo) {
        if (corporationRepository.existsByRegNo(regNo)) {
            throw new DuplicateResourceException("이미 등록된 사업자등록번호입니다.");
        }
    }

    private String uploadOrThrow(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("필수 서류 파일이 비어 있습니다.");
        }

        return s3Manager.upload(file, prefix);
    }
}
