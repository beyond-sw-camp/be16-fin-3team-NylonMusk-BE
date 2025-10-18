package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisclosureAdminQueryService {

    private final DisclosureRepository disclosureRepository;

    public Page<DisclosureResDto> search(
            DisclosureStatus status,
            DisclosureType type,
            UUID stockId,
            String title,
            LocalDateTime from,
            LocalDateTime toExclusive,
            Pageable pageable
    ) {
        return disclosureRepository.searchAdmin(status, type, stockId, title, from, toExclusive, pageable)
                .map(DisclosureMapper::toRes);
    }
}
