package com.beyond.MKX.domain.securities_firm.service;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.securities_firm.dto.SecuritiesFirmDetailResDto;
import com.beyond.MKX.domain.securities_firm.dto.SecuritiesFirmListResDto;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import com.beyond.MKX.domain.securities_firm.repository.SecuritiesFirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SecuritiesFirmAdminQueryService {

    private final SecuritiesFirmRepository securitiesFirmRepository;
    private final AdminRepository adminRepository;

    public Page<SecuritiesFirmListResDto> list(String q, String status, Pageable pageable) {
        SecuritiesFirm.Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = SecuritiesFirm.Status.valueOf(status.toUpperCase());
        }
        String qNorm = (q == null || q.isBlank()) ? null : q;
        return securitiesFirmRepository.searchAdmin(statusEnum, qNorm, pageable)
                .map(s -> {
                    Optional<Admin> a = adminRepository.findBySecuritiesFirm(s);
                    return SecuritiesFirmListResDto.from(
                            s,
                            a.map(Admin::getName).orElse(null),
                            a.map(Admin::getEmail).orElse(null),
                            a.map(Admin::getPhone).orElse(null)
                    );
                });
    }

    public SecuritiesFirmDetailResDto detail(UUID id) {
        SecuritiesFirm s = securitiesFirmRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("증권사를 찾을 수 없습니다."));
        Optional<Admin> a = adminRepository.findBySecuritiesFirm(s);
        return SecuritiesFirmDetailResDto.from(
                s,
                a.map(Admin::getName).orElse(null),
                a.map(Admin::getEmail).orElse(null),
                a.map(Admin::getPhone).orElse(null)
        );
    }
}

