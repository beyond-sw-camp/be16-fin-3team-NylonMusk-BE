package com.beyond.MKX.domain.corporation.service;

import com.beyond.MKX.common.exception.DuplicateResourceException;
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

@Service
@RequiredArgsConstructor
@Transactional
public class CorporationService {

    private final CorporationRepository corporationRepository;
    private final AdminSignUpService adminSignUpService;


    public Admin signUpAdmin(CorporationSignUpReqDto request) {
        validateDuplicateRegNo(request.getRegNo());

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
                .businessRegistrationCert(request.getBusinessRegistrationCert())
                .corporateSealCert(request.getCorporateSealCert())
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
}
