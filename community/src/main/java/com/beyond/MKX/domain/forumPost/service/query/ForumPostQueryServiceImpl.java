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

    private final ForumPostRepository forumPostRepository;

    @Override
    public Page<ForumPostResDto> list(PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? forumPostRepository.findAllBy(pageable) // <- 일관성 위해 findAllBy 사용
                : forumPostRepository.findByStatus(status, pageable);
        return page.map(this::map);
    }

    @Override
    public Page<ForumPostResDto> listMine(UUID me, PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? forumPostRepository.findByCreatedBy(me, pageable)
                : forumPostRepository.findByCreatedByAndStatus(me, status, pageable);
        return page.map(this::map);
    }

    @Override
    public Page<ForumPostResDto> listByUser(UUID userId, PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? forumPostRepository.findByCreatedBy(userId, pageable)
                : forumPostRepository.findByCreatedByAndStatus(userId, status, pageable);
        return page.map(this::map);
    }

    @Override
    public ForumPostResDto get(UUID postId) {
        ForumPost p = forumPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));
        return map(p);
    }

    private ForumPostResDto map(ForumPost p) {
        List<ForumCategoryResDto> catDtos = p.getCategories().stream()
                .map(ForumPostCategory::getCategory)
                .map(c -> ForumCategoryResDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
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
                .writerRole(p.getWriterRole())
                .build();
    }
}
