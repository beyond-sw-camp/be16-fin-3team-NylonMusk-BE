// com/beyond/MKX/domain/forumPost/service/query/ForumPostQueryServiceImpl.java
package com.beyond.MKX.domain.forumPost.service.query;

import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.entity.ForumPostCategory;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumPostQueryServiceImpl implements ForumPostQueryService {

    private final ForumPostRepository repo;

    @Override
    public Page<ForumPostResDto> list(PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? repo.findAll(pageable)
                : repo.findByStatus(status, pageable);
        return page.map(this::map);
    }

    @Override
    public Page<ForumPostResDto> listMine(UUID me, PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? repo.findByCreatedBy(me, pageable)
                : repo.findByCreatedByAndStatus(me, status, pageable);
        return page.map(this::map);
    }

    @Override
    public Page<ForumPostResDto> listByUser(UUID userId, PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? repo.findByCreatedBy(userId, pageable)
                : repo.findByCreatedByAndStatus(userId, status, pageable);
        return page.map(this::map);
    }

    @Override
    public ForumPostResDto get(UUID postId) {
        ForumPost p = repo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));
        // @SQLRestriction로 이미 삭제글은 조회 안 됨(추가 체크는 선택)
        return map(p);
    }

    private ForumPostResDto map(ForumPost p) {
        List<ForumCategoryResDto> catDtos = p.getCategories().stream()
                .map(ForumPostCategory::getCategory)
                .map(c -> ForumCategoryResDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .createdBy(c.getCreatedBy())
                        .createdAt(c.getCreatedAt())
                        .updatedAt(c.getUpdatedAt())
                        .deletedAt(c.getDeletedAt())
                        .version(c.getVersion())
                        .build()
                )
                .toList();

        return ForumPostResDto.builder()
                .id(p.getId())
                .stockId(p.getStockId())
                .createdBy(p.getCreatedBy())
                .title(p.getTitle())
                .contents(p.getContents())
                .imageUrl(p.getImageUrl())
                .status(p.getStatus())
                .likesCount(p.getLikesCount())
                .commentCount(p.getCommentCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .deletedAt(p.getDeletedAt())
                .version(p.getVersion())
                .categories(catDtos)
                .writtenByAdmin(p.isWrittenByAdmin())
                .build();
    }
}
