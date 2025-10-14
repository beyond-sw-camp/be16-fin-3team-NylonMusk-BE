package com.beyond.MKX.domain.forumCategory.service.command;

import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryCreateReqDto;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryUpdateReqDto;
import com.beyond.MKX.domain.forumCategory.entity.ForumCategory;
import com.beyond.MKX.domain.forumCategory.repository.ForumCategoryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumCategoryCommandServiceImpl implements ForumCategoryCommandService {

    private final ForumCategoryRepository repo;

    @Override
    public ForumCategoryResDto create(ForumCategoryCreateReqDto req) {
        if (repo.existsByName(req.name())) {
            throw new IllegalArgumentException("이미 존재하는 카테고리명입니다: " + req.name());
        }

        ForumCategory e = ForumCategory.builder()
                .name(req.name())
                .description(req.description())
                .createdBy(req.createdBy())
                .build();

        ForumCategory saved = repo.save(e);
        return map(saved);
    }

    @Override
    public ForumCategoryResDto update(UUID id, ForumCategoryUpdateReqDto req) {
        // 삭제 포함으로 조회(복구/이력 유지 가정). 활성만 허용하려면 findActiveById 사용
        ForumCategory e = repo.findOneIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("ForumCategory not found: " + id));

        if (req.name() != null && !req.name().equals(e.getName())) {
            // (선택) 이름 변경 시 중복 체크
            if (repo.existsByName(req.name())) {
                throw new IllegalArgumentException("이미 존재하는 카테고리명입니다: " + req.name());
            }
            e.setName(req.name());
        }
        if (req.description() != null) {
            e.setDescription(req.description());
        }

        try {
            return map(repo.save(e)); // @Version으로 낙관적 락 검증
        } catch (OptimisticLockingFailureException ex) {
            throw new IllegalStateException("동시 수정 충돌이 발생했습니다. 다시 시도하세요.", ex);
        }
    }

    @Override
    public void softDelete(UUID id) {
        ForumCategory e = repo.findOneIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("ForumCategory not found: " + id));

        if (e.getDeletedAt() == null) {
            e.markDeleted();         // UPDATE 경로 → @Version 검증이 적용됨
            repo.save(e);
        }
        // 이미 삭제된 경우는 멱등 처리
    }

    @Override
    public ForumCategoryResDto restore(UUID id) {
        ForumCategory e = repo.findOneIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("ForumCategory not found: " + id));

        if (e.getDeletedAt() != null) {
            e.restore();             // UPDATE 경로 → @Version 검증
            e = repo.save(e);
        }
        return map(e);
    }

    private static ForumCategoryResDto map(ForumCategory e) {
        return new ForumCategoryResDto(
                e.getId(),
                e.getName(),
                e.getDescription(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getDeletedAt(),
                e.getVersion()
        );
    }
}
