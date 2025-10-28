package com.beyond.MKX.domain.delisting.service.query;

import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaResDto;

import java.util.List;
import java.util.UUID;

public interface DelistingCriteriaQueryService {
    DelistingCriteriaResDto get(UUID id);
    List<DelistingCriteriaResDto> list(String deleted);
    List<DelistingCriteriaResDto> listByType(com.beyond.MKX.domain.delisting.entity.CriteriaType criteriaType);
}
