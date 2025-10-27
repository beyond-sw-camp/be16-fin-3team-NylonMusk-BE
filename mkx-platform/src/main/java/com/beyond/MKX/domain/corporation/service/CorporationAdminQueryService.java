package com.beyond.MKX.domain.corporation.service;

import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.corporation.dto.CorporationDetailResDto;
import com.beyond.MKX.domain.corporation.dto.CorporationListResDto;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.entity.Status;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
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
public class CorporationAdminQueryService {

    private final CorporationRepository corporationRepository;
    private final AdminRepository adminRepository;

    public Page<CorporationListResDto> list(String q, String status, Pageable pageable) {
        Status statusEnum = null;
        if (status != null && !status.isBlank()) {
            statusEnum = Status.valueOf(status.toUpperCase());
        }
        String qNorm = (q == null || q.isBlank()) ? null : q;
        return corporationRepository.search(statusEnum, qNorm, pageable)
                .map(c -> {
                    Optional<Admin> a = adminRepository.findByCorporation(c);
                    return CorporationListResDto.from(
                            c,
                            a.map(Admin::getName).orElse(null),
                            a.map(Admin::getEmail).orElse(null),
                            a.map(Admin::getPhone).orElse(null)
                    );
                });
    }

    public CorporationDetailResDto detail(UUID id) {
        Corporation c = corporationRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("기업을 찾을 수 없습니다."));
        Optional<Admin> adminOpt = adminRepository.findByCorporation(c);
        return CorporationDetailResDto.from(
                c,
                adminOpt.map(Admin::getName).orElse(null),
                adminOpt.map(Admin::getEmail).orElse(null),
                adminOpt.map(Admin::getPhone).orElse(null)
        );
    }
}
