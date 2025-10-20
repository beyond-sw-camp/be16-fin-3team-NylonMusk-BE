package com.beyond.MKX.domain.forumPostComment.service;

import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import com.beyond.MKX.domain.forumPostComment.dto.*;
import com.beyond.MKX.domain.forumPostComment.entity.ForumPostComment;
import com.beyond.MKX.domain.forumPostComment.entity.ForumPostCommentLike;
import com.beyond.MKX.domain.forumPostComment.repository.ForumPostCommentLikeRepository;
import com.beyond.MKX.domain.forumPostComment.repository.ForumPostCommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumPostCommentCommandServiceImpl implements ForumPostCommentCommandService {

    private final ForumPostCommentRepository commentRepo;
    private final ForumPostCommentLikeRepository likeRepo;
    private final ForumPostRepository postRepo;

    @Override
    public ForumPostCommentRes create(UUID requesterId, ForumPostCommentCreateReq req) {
        ForumPost post = postRepo.findById(req.postId())
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + req.postId()));

        ForumPostComment entity = ForumPostComment.builder()
                .post(post)
                .createdBy(requesterId)
                .content(req.content())
                .likes(0)
                .build();

        ForumPostComment saved = commentRepo.save(entity);

        postRepo.incrementCommentCount(post.getId());

        return ForumPostCommentMapper.toRes(saved);
    }

    @Override
    public ForumPostCommentRes update(UUID requesterId, UUID commentId, ForumPostCommentUpdateReq req) {
        ForumPostComment c = commentRepo.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        if (!c.getCreatedBy().equals(requesterId)) {
            throw new SecurityException("No permission to update this comment");
        }
        c.setContent(req.content());
        return ForumPostCommentMapper.toRes(c);
    }

    @Override
    public void delete(UUID requesterId, UUID commentId) {
        ForumPostComment c = commentRepo.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        if (!c.getCreatedBy().equals(requesterId)) {
            throw new SecurityException("No permission to delete this comment");
        }
        int updated = commentRepo.softDelete(commentId, LocalDateTime.now());
        if (updated == 0) throw new IllegalStateException("Already deleted or cannot delete: " + commentId);

        postRepo.decrementCommentCount(c.getPost().getId());
    }

    @Override
    public CommentLikeToggleRes toggleLike(UUID requesterId, UUID commentId) {
        // 존재 & 삭제여부 확인
        ForumPostComment comment = commentRepo.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));

        // 이미 좋아요?
        var existing = likeRepo.findByCommentIdAndUserId(commentId, requesterId);
        boolean liked;
        int updated;

        if (existing.isPresent()) {
            // 취소
            likeRepo.deleteByCommentIdAndUserId(commentId, requesterId);
            commentRepo.decrementLikes(commentId);
            liked = false;
        } else {
            // 추가 (경쟁상황 대비 UNIQUE 충돌 처리)
            try {
                ForumPostCommentLike like = ForumPostCommentLike.builder()
                        .comment(comment)
                        .userId(requesterId)
                        .build();
                likeRepo.save(like);
                commentRepo.incrementLikes(commentId);
                liked = true;
            } catch (DataIntegrityViolationException ex) {
                // 동시성으로 UNIQUE 충돌 시 재시도 분기: 이미 생성된 것으로 보고 취소 처리할지, 유지할지 선택
                // 여기서는 "이미 생성"으로 판단하고 최신 카운트만 반환
                liked = true;
            }
        }

        int likeCount = commentRepo.findById(commentId)
                .map(ForumPostComment::getLikes)
                .orElse(0);

        return CommentLikeToggleRes.builder()
                .liked(liked)
                .likeCount(likeCount)
                .build();
    }
}
