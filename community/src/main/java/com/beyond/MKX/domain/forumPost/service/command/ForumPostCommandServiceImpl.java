package com.beyond.MKX.domain.forumPost.service.command;

import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostStatusUpdateReq;
import com.beyond.MKX.domain.forumPost.dto.ForumPostUpdateReq;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumPostCommandServiceImpl implements ForumPostCommandService {

    private final ForumPostRepository repo;

    @Override
    public ForumPostResDto update(UUID postId, UUID actorId, String actorRole, ForumPostUpdateReq req) {
        ForumPost post = repo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        // 권한: 작성자만 가능 (관리자라도 여기서는 내용 수정 허용 안 함)
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

        // 소프트 삭제: 엔티티의 @SQLDelete를 직접 쓰기보다 버전 검증을 위해 UPDATE 경로로 처리 권장
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

        // 관리자만 임의 상태 변경 가능
        post.setStatus(req.status() == null ? PostStatus.UPDATED : req.status());

        ForumPost saved = repo.save(post);
        return map(saved);
    }

    private boolean isAdmin(String role) {
        return role != null && role.toUpperCase(Locale.ROOT).contains("ADMIN");
    }

    private ForumPostResDto map(ForumPost p) {
        return new ForumPostResDto(
                p.getId(), p.getStockId(), p.getCreatedBy(), p.getTitle(), p.getContents(),
                p.getImageUrl(), p.getStatus(), p.getLikesCount(), p.getCommentCount(),
                p.getCreatedAt(), p.getUpdatedAt(), p.getDeletedAt(), p.getVersion()
        );
    }
}
