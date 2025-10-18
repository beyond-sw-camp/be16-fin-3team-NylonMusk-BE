package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisclosureCorpQueryService {

    private final DisclosureRepository disclosureRepository;
    private final AdminRepository adminRepository;

    public Page<DisclosureResDto> listMine(
            DisclosureStatus status,
            DisclosureType type,
            String title,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        UUID corpId = currentCorporationId();
        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime toExclusive = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;
        return disclosureRepository.searchByCorporation(corpId, status, type, title, from, toExclusive, pageable)
                .map(DisclosureMapper::toRes);
    }

    private UUID currentCorporationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomAdminPrincipal principal)) {
            throw new IllegalArgumentException("인증되지 않은 요청입니다.");
        }
        Admin admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        if (admin.getCorporation() == null) {
            throw new IllegalArgumentException("기업 관리자 계정이 아닙니다.");
        }
        return admin.getCorporation().getId();
    }
}

