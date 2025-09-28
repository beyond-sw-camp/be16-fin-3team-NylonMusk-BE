package com.beyond.MKX.domain.organization.service;

import com.beyond.MKX.common.exception.DuplicateResourceException;
import com.beyond.MKX.domain.organization.entity.Organization;
import com.beyond.MKX.domain.organization.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class OrganizationService {
    private final OrganizationRepository organizationRepository;

    public Organization findByRegNo (String regNo) {
        return organizationRepository.findByRegNo(regNo).orElseThrow(() ->
                new DuplicateResourceException("중복된 사업자등록번호입니다."));
    }
}
