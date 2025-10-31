package com.beyond.MKX.domain.delisting.service.command;

import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaCreateReqDto;
import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaResDto;
import com.beyond.MKX.domain.delisting.dto.DelistingCriteriaUpdateReqDto;
import com.beyond.MKX.domain.delisting.entity.DelistingCriteria;
import com.beyond.MKX.domain.delisting.repository.DelistingCriteriaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DelistingCriteriaCommandServiceImpl implements DelistingCriteriaCommandService {

    private final DelistingCriteriaRepository repo;

    @Override
    public DelistingCriteriaResDto create(DelistingCriteriaCreateReqDto req) {
        if (repo.existsByCriteriaCode(req.criteriaCode())) {
            throw new IllegalArgumentException("이미 존재하는 기준 코드입니다: " + req.criteriaCode());
        }

        DelistingCriteria entity = DelistingCriteria.builder()
                .criteriaCode(req.criteriaCode())
                .criteriaName(req.criteriaName())
                .criteriaType(req.criteriaType())
                .thresholdValue(req.thresholdValue())
                .comparisonOperator(req.comparisonOperator())
                .thresholdPeriod(req.thresholdPeriod())
                .thresholdUnit(req.thresholdUnit())
                .description(req.description())
                .isActive(req.isActive() != null ? req.isActive() : true)
                .createdBy(req.createdBy())
                .effectiveFrom(req.effectiveFrom())
                .effectiveTo(req.effectiveTo())
                .build();

        DelistingCriteria saved = repo.save(entity);
        return map(saved);
    }

    @Override
    public DelistingCriteriaResDto update(UUID id, DelistingCriteriaUpdateReqDto req) {
        DelistingCriteria entity = repo.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("DelistingCriteria not found: " + id));

        try {
            // 부분 업데이트 (null이 아닌 필드만 업데이트)
            if (req.criteriaName() != null) entity.setCriteriaName(req.criteriaName());
            if (req.criteriaType() != null) entity.setCriteriaType(req.criteriaType());
            if (req.thresholdValue() != null) entity.setThresholdValue(req.thresholdValue());
            if (req.comparisonOperator() != null) entity.setComparisonOperator(req.comparisonOperator());
            if (req.thresholdPeriod() != null) entity.setThresholdPeriod(req.thresholdPeriod());
            if (req.thresholdUnit() != null) entity.setThresholdUnit(req.thresholdUnit());
            if (req.description() != null) entity.setDescription(req.description());
            if (req.isActive() != null) entity.setIsActive(req.isActive());
            if (req.updatedBy() != null) entity.setUpdatedBy(req.updatedBy());
            if (req.effectiveFrom() != null) entity.setEffectiveFrom(req.effectiveFrom());
            if (req.effectiveTo() != null) entity.setEffectiveTo(req.effectiveTo());

            DelistingCriteria saved = repo.save(entity);
            return map(saved);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("다른 사용자가 동시에 수정했습니다. 다시 시도해주세요.");
        }
    }

    @Override
    public void softDelete(UUID id) {
        DelistingCriteria entity = repo.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("DelistingCriteria not found: " + id));
        
        entity.markDeleted();
        repo.save(entity);
    }

    @Override
    public DelistingCriteriaResDto restore(UUID id) {
        DelistingCriteria entity = repo.findOneIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("DelistingCriteria not found: " + id));
        
        if (entity.getDeletedAt() == null) {
            throw new IllegalStateException("이미 활성화된 기준입니다.");
        }
        
        entity.restore();
        DelistingCriteria saved = repo.save(entity);
        return map(saved);
    }

    private DelistingCriteriaResDto map(DelistingCriteria entity) {
        return DelistingCriteriaResDto.builder()
                .id(entity.getId())
                .criteriaCode(entity.getCriteriaCode())
                .criteriaName(entity.getCriteriaName())
                .criteriaType(entity.getCriteriaType())
                .thresholdValue(entity.getThresholdValue())
                .comparisonOperator(entity.getComparisonOperator())
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
