package com.beyond.MKX.domain.forumPost.service.command;

import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumCategory.entity.ForumCategory;
import com.beyond.MKX.domain.forumCategory.repository.ForumCategoryRepository;
import com.beyond.MKX.domain.forumPost.dto.ForumPostCreateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostStatusUpdateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostUpdateReq;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.entity.ForumPostCategory;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumPostCommandServiceImpl implements ForumPostCommandService {

    private final ForumPostRepository repo;
    private final ForumCategoryRepository categoryRepo;
    private final S3Manager s3Manager;

    @Override
    public ForumPostResDto create(UUID actorId, String actorRole, ForumPostCreateReq req) {
        if (req.categoryIds() == null || req.categoryIds().isEmpty()) {
            throw new IllegalArgumentException("카테고리를 최소 1개 이상 선택하세요.");
        }
        var catIds = req.categoryIds().stream().distinct().toList();
        List<ForumCategory> cats = categoryRepo.findAllById(catIds);
        if (cats.size() != catIds.size()) {
            throw new IllegalArgumentException("유효하지 않은 카테고리 ID가 포함되어 있습니다.");
        }

        // prefix = forum-posts/{stockId}/{대표카테고리슬러그}
        // 대표카테고리: 이름 기준 오름차순 첫 번째(일관성 위해) 또는 목록 첫 번째
        String primaryName = cats.stream()
                .map(ForumCategory::getName)
                .min(Comparator.naturalOrder())
                .orElse("uncategorized");
        String primaryCategorySlug = slugify(primaryName);
        String prefix = String.format("forum-posts/%s/%s", req.stockId(), primaryCategorySlug);

        String imageUrl = null;
        MultipartFile f = req.imageFile();
        if (f != null && !f.isEmpty()) {
            validateImage(f);
            imageUrl = s3Manager.upload(f, prefix);
        }

        boolean admin = isAdmin(actorRole);

        ForumPost post = ForumPost.builder()
                .stockId(req.stockId())
                .createdBy(actorId)
                .writtenByAdmin(admin)
                .title(req.title())
                .contents(req.contents())
                .imageUrl(imageUrl)
                .build();
        post.replaceCategories(cats);

        try {
            ForumPost saved = repo.save(post);
            return map(saved);
        } catch (RuntimeException e) {
            if (imageUrl != null) {
                try { s3Manager.delete(imageUrl); } catch (Exception ignore) {}
            }
            throw e;
        }
    }

    private void validateImage(MultipartFile f) {
        String ct = (f.getContentType() == null) ? "" : f.getContentType().toLowerCase(Locale.ROOT);
        if (!ct.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }
        long max = 5L * 1024 * 1024; // 5MB
        if (f.getSize() > max) {
            throw new IllegalArgumentException("이미지 파일은 5MB 이하여야 합니다.");
        }
    }

    private String slugify(String input) {
        if (input == null || input.isBlank()) return "uncategorized";
        String s = input.strip().toLowerCase(Locale.ROOT);
        s = s.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\- _]", "");
        s = s.replaceAll("\\s+", "-");
        s = s.replaceAll("-{2,}", "-");
        s = s.replaceAll("(^-|-$)", "");
        if (s.length() > 50) s = s.substring(0, 50);
        return s.isEmpty() ? "uncategorized" : s;
    }

    @Override
    public ForumPostResDto update(UUID postId, UUID actorId, String actorRole, ForumPostUpdateReq req) {
        ForumPost post = repo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        if (!post.getCreatedBy().equals(actorId)) {
            throw new SecurityException("작성자만 수정할 수 있습니다.");
        }

        post.updateContents(req.title(), req.contents(), req.imageUrl());

        try {
            ForumPost saved = repo.save(post);
            return map(saved);
        } catch (OptimisticLockingFailureException e) {
            throw new IllegalStateException("동시 수정 충돌이 발생했습니다. 다시 시도해주세요.", e);
        }
    }

    @Override
    public void delete(UUID postId, UUID actorId, String actorRole) {
        ForumPost post = repo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        if (!isAdmin(actorRole) && !post.getCreatedBy().equals(actorId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        if (post.getDeletedAt() == null) {
            post.markDeleted();
            repo.save(post);
        }
    }

    @Override
    public ForumPostResDto updateStatus(UUID postId, String actorRole, ForumPostStatusUpdateReq req) {
        if (!isAdmin(actorRole)) {
            throw new SecurityException("상태 변경 권한이 없습니다(관리자 전용).");
        }
        ForumPost post = repo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        post.setStatus(req.status() == null ? PostStatus.UPDATED : req.status());

        ForumPost saved = repo.save(post);
        return map(saved);
    }

    private boolean isAdmin(String role) {
        return role != null && role.toUpperCase(Locale.ROOT).contains("ADMIN");
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
