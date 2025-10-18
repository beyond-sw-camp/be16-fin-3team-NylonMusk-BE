package com.beyond.MKX.domain.disclosure.mapper;

import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;

public final class DisclosureMapper {
    private DisclosureMapper() {}

    public static DisclosureResDto toRes(Disclosure e) {
        return new DisclosureResDto(
                e.getId(),
                e.getStockId(),
                e.getDisclosureType(),
                e.getTitle(),
                e.getSummary(),
                e.getFileUrl(),
                e.getStatus(),
                e.getPublishedAt(),
                e.getStockNameSnapshot(),
                e.getTickerSnapshot(),
                e.getRejectCode(),
                e.getRejectReason(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
