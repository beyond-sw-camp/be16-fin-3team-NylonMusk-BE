package com.beyond.MKX.domain.forumPost.service.command;

import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.common.entity.WriterRole;
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

    private final ForumPostRepository forumPostRepository;
    private final ForumCategoryRepository forumCategoryRepository;
    private final S3Manager s3Manager;

    @Override
    public ForumPostResDto create(UUID actorId, String actorRole, ForumPostCreateReq req) {
        if (req.categoryIds() == null || req.categoryIds().isEmpty()) {
            throw new IllegalArgumentException("카테고리를 최소 1개 이상 선택하세요.");
        }
        var catIds = req.categoryIds().stream().distinct().toList();
        List<ForumCategory> cats = forumCategoryRepository.findAllById(catIds);
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

        ForumPost post = ForumPost.builder()
                .stockId(req.stockId())
                .createdBy(actorId)
                .writerRole(WriterRole.valueOf(actorRole))
                .title(req.title())
                .contents(req.contents())
                .imageUrl(imageUrl)
                .build();
        post.replaceCategories(cats);

        try {
            ForumPost saved = forumPostRepository.save(post);
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
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        // 작성자만 수정 가능
        if (!post.getCreatedBy().equals(actorId)) {
            throw new SecurityException("작성자만 수정할 수 있습니다.");
        }

        // ===== 카테고리 교체 =====
        List<ForumCategory> newCats = null;
        if (req.categoryIds() != null) {
            if (req.categoryIds().isEmpty()) {
                throw new IllegalArgumentException("카테고리를 최소 1개 이상 선택하세요.");
            }
            var distinctIds = req.categoryIds().stream().distinct().toList();
            newCats = forumCategoryRepository.findAllById(distinctIds);
            if (newCats.size() != distinctIds.size()) {
                throw new IllegalArgumentException("유효하지 않은 카테고리 ID가 포함되어 있습니다.");
            }

            // 동일한 구성이면 skip (불필요한 delete/insert 방지)
            var existingIds = post.getCategories().stream()
                    .map(pc -> pc.getCategory().getId())
                    .sorted().toList();
            var incomingIds = newCats.stream().map(ForumCategory::getId)
                    .sorted().toList();
            if (!existingIds.equals(incomingIds)) {
                // 1) 모두 제거 + flush (DB에서 delete 먼저 수행)
                post.getCategories().clear();
                forumPostRepository.saveAndFlush(post);
                // 2) 새로 추가
                for (ForumCategory c : newCats) {
                    post.getCategories().add(
                            ForumPostCategory.builder().post(post).category(c).build()
                    );
                }
            }
        }

        // ===== 이미지 처리 =====
        String oldUrl = post.getImageUrl();
        String uploadedUrl = null;

        List<ForumCategory> baseCats = (newCats != null)
                ? newCats
                : post.getCategories().stream().map(ForumPostCategory::getCategory).toList();

        String primarySlug = slugify(
                baseCats.stream()
                        .map(ForumCategory::getName)
                        .min(Comparator.naturalOrder())
                        .orElse("uncategorized")
        );

        MultipartFile newImage = req.imageFile();
        if (newImage != null && !newImage.isEmpty()) {
            validateImage(newImage);
            String prefix = String.format("forum-posts/%s/%s", post.getStockId(), primarySlug);
            uploadedUrl = s3Manager.upload(newImage, prefix);
            post.setImageUrl(uploadedUrl); // 엔티티에 새로운 URL 반영
        } else if (Boolean.TRUE.equals(req.removeImage())) {
            post.setImageUrl(null); // 업로드 없고 제거 플래그가 true면 제거
        }

        // 제목/본문은 제공된 것만 반영 (imageUrl은 위에서 처리)
        post.updateContents(req.title(), req.contents(), null);

        try {
            ForumPost saved = forumPostRepository.save(post);

            // 교체 성공 시 기존 이미지 정리(베스트 에포트)
            if (uploadedUrl != null && oldUrl != null && !oldUrl.equals(uploadedUrl)) {
                try { s3Manager.delete(oldUrl); } catch (Exception ignore) {}
            }
            // 제거만 한 경우에도 기존 이미지 정리
            if (uploadedUrl == null && Boolean.TRUE.equals(req.removeImage()) && oldUrl != null) {
                try { s3Manager.delete(oldUrl); } catch (Exception ignore) {}
            }

            return map(saved);
        } catch (OptimisticLockingFailureException e) {
            // DB 저장 실패 시 새로 업로드한 파일 롤백
            if (uploadedUrl != null) {
                try { s3Manager.delete(uploadedUrl); } catch (Exception ignore) {}
            }
            throw new IllegalStateException("동시 수정 충돌이 발생했습니다. 다시 시도해주세요.", e);
        } catch (RuntimeException e) {
            if (uploadedUrl != null) {
                try { s3Manager.delete(uploadedUrl); } catch (Exception ignore) {}
            }
            throw e;
        }
    }

    @Override
    public void delete(UUID postId, UUID actorId, String actorRole) {
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        if (!isAdmin(actorRole) && !post.getCreatedBy().equals(actorId)) {
            throw new SecurityException("삭제 권한이 없습니다.");
        }

        if (post.getDeletedAt() == null) {
            post.markDeleted();
            forumPostRepository.save(post);
        }
    }

    @Override
    public ForumPostResDto updateStatus(UUID postId, String actorRole, ForumPostStatusUpdateReq req) {
        if (!isAdmin(actorRole)) {
            throw new SecurityException("상태 변경 권한이 없습니다(관리자 전용).");
        }
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        post.setStatus(req.status() == null ? PostStatus.UPDATED : req.status());

        ForumPost saved = forumPostRepository.save(post);
        return map(saved);
    }

    private boolean isAdmin(String role) {
        return role != null && role.toUpperCase(Locale.ROOT).contains("EXCHANGE");
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
                .writerRole(p.getWriterRole())
                .build();
    }
}
