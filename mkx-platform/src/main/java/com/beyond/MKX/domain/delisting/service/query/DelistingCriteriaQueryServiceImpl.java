package com.beyond.MKX.domain.delisting.service.query;

import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaResDto;
import com.beyond.MKX.domain.delisting.entity.DelistingCriteria;
import com.beyond.MKX.domain.delisting.repository.DelistingCriteriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DelistingCriteriaQueryServiceImpl implements DelistingCriteriaQueryService {

    private final DelistingCriteriaRepository repo;

    @Override
    public DelistingCriteriaResDto get(UUID id) {
        DelistingCriteria entity = repo.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("DelistingCriteria not found: " + id));
        return map(entity);
    }

    @Override
    public List<DelistingCriteriaResDto> list(String deleted) {
        return switch (deleted) {
            case "all" -> repo.findAll().stream().map(this::map).toList();
            case "only" -> repo.findAll().stream()
                    .filter(e -> e.getDeletedAt() != null)
                    .map(this::map).toList();
            default -> repo.findAllActive().stream().map(this::map).toList();
        };
    }

    @Override
    public List<DelistingCriteriaResDto> listByType(com.beyond.MKX.domain.delisting.entity.CriteriaType criteriaType) {
        return repo.findByCriteriaTypeAndActive(criteriaType).stream()
                .map(this::map).toList();
    }

    private DelistingCriteriaResDto map(DelistingCriteria entity) {
        return DelistingCriteriaResDto.builder()
                .id(entity.getId())
                .criteriaCode(entity.getCriteriaCode())
                .criteriaName(entity.getCriteriaName())
                .criteriaType(entity.getCriteriaType())
                .thresholdValue(entity.getThresholdValue())
                .thresholdPeriod(entity.getThresholdPeriod())
                .thresholdUnit(entity.getThresholdUnit())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .effectiveFrom(entity.getEffectiveFrom())
                .effectiveTo(entity.getEffectiveTo())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .version(entity.getVersion())
                .build();
    }
}
