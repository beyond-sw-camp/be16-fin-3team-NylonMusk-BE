package com.beyond.MKX.domain.delisting.service.command;

import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaCreateReqDto;
import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaResDto;
import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaUpdateReqDto;

import java.util.UUID;

public interface DelistingCriteriaCommandService {
    DelistingCriteriaResDto create(DelistingCriteriaCreateReqDto req);
    DelistingCriteriaResDto update(UUID id, DelistingCriteriaUpdateReqDto req);
    void softDelete(UUID id);
    DelistingCriteriaResDto restore(UUID id);
}
