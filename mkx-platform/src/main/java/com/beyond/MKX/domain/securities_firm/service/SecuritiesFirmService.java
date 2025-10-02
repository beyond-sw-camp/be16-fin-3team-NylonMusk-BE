package com.beyond.MKX.domain.securities_firm.service;

import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.service.AdminSignUpService;
import com.beyond.MKX.domain.securities_firm.dto.SecuritiesFirmSignUpReqDto;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm.Status;
import com.beyond.MKX.domain.securities_firm.repository.SecuritiesFirmRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SecuritiesFirmService {

    private final SecuritiesFirmRepository securitiesFirmRepository;
    private final AdminSignUpService adminSignUpService;
    private final S3Manager s3Manager;

    @Transactional
    public Admin signUpAdmin(SecuritiesFirmSignUpReqDto request) {
        validateDuplicate(request);

        String financialInvestmentLicenseDocUrl = uploadOrThrow(
                request.getFinancialInvestmentLicenseDocFile(),
                "securities-firms/license"
        );

        String businessRegistrationCertUrl = uploadOrThrow(
                request.getBusinessRegistrationCertFile(),
                "securities-firms/business-registration"
        );

        String corporateSealCertUrl = uploadOrThrow(
                request.getCorporateSealCertFile(),
                "securities-firms/corporate-seal"
        );

        SecuritiesFirm securitiesFirm = SecuritiesFirm.builder()
                .nameKo(request.getNameKo())
                .nameEng(request.getNameEng())
                .ownerName(request.getOwnerName())
                .regNo(request.getRegNo())
                .status(Status.PENDING)
                .establishedDate(request.getEstablishedDate())
                .roadAddress(request.getRoadAddress())
                .detailAddress(request.getDetailAddress())
                .financialInvestmentLicenseNo(request.getFinancialInvestmentLicenseNo())
                .financialInvestmentLicenseDoc(financialInvestmentLicenseDocUrl)
                .businessRegistrationCert(businessRegistrationCertUrl)
                .corporateSealCert(corporateSealCertUrl)
                .exchangeFee(request.getExchangeFee())
                .build();

        SecuritiesFirm savedFirm = securitiesFirmRepository.save(securitiesFirm);

        return adminSignUpService.createPendingAdmin(
                request.getAdminEmail(),
                request.getAdminPhone(),
                request.getAdminName(),
                request.getAdminPassword(),
                Role.BROKERAGE,
                null,
                savedFirm
        );
    }

    private void validateDuplicate(SecuritiesFirmSignUpReqDto request) {
        if (securitiesFirmRepository.existsByRegNo(request.getRegNo())) {
            throw new DuplicateResourceException("이미 등록된 사업자등록번호입니다.");
        }

        if (securitiesFirmRepository.existsByFinancialInvestmentLicenseNo(request.getFinancialInvestmentLicenseNo())) {
            throw new DuplicateResourceException("이미 등록된 금융투자업 인가번호입니다.");
        }
    }

    private String uploadOrThrow(MultipartFile file, String prefix) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("필수 서류 파일이 비어 있습니다.");
        }

        return s3Manager.upload(file, prefix);
    }
}
