package com.beyond.MKX.domain.forumCategory.service.query;

import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumCategory.entity.ForumCategory;
import com.beyond.MKX.domain.forumCategory.repository.ForumCategoryQueryRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumCategoryQueryServiceImpl implements ForumCategoryQueryService {

    private final ForumCategoryQueryRepository repo;

    @Override
    public ForumCategoryResDto get(UUID id) {
        ForumCategory e = repo.findOneIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("ForumCategory not found: " + id));
        return map(e);
    }

    @Override
    public Page<ForumCategoryResDto> list(String deleted, Pageable pageable) {
        return switch (deleted) {
            case "all"  -> repo.findAllIncludingDeleted(pageable).map(this::map);
            case "only" -> repo.findDeletedOnly(pageable).map(this::map);
            default     -> repo.findActiveOnly(pageable).map(this::map); // exclude
        };
    }

    @Override
    public Page<ForumCategoryResDto> listByUser(UUID userUuid, String userId, String deleted, Pageable pageable) {
        if (userUuid != null) {
            return switch (deleted) {
                case "all"  -> repo.findAllByCreator(userUuid, pageable).map(this::map);
                case "only" -> repo.findDeletedByCreator(userUuid, pageable).map(this::map);
                default     -> repo.findActiveByCreator(userUuid, pageable).map(this::map);
            };
        }
        if (userId != null && !userId.isBlank()) {
            return switch (deleted) {
                case "all"  -> repo.findAllByCreatorUserId(userId, pageable).map(this::map);
                case "only" -> repo.findDeletedByCreatorUserId(userId, pageable).map(this::map);
                default     -> repo.findActiveByCreatorUserId(userId, pageable).map(this::map);
            };
        }
        return Page.empty(pageable);
    }

    private ForumCategoryResDto map(ForumCategory e) {
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
