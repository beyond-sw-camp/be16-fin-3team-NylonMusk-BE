package com.beyond.MKX.domain.forumPostComment.service;

import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import com.beyond.MKX.domain.forumPostComment.dto.ForumPostCommentMapper;
import com.beyond.MKX.domain.forumPostComment.dto.ForumPostCommentRes;
import com.beyond.MKX.domain.forumPostComment.entity.ForumPostComment;
import com.beyond.MKX.domain.forumPostComment.repository.ForumPostCommentLikeRepository;
import com.beyond.MKX.domain.forumPostComment.repository.ForumPostCommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumPostCommentQueryServiceImpl implements ForumPostCommentQueryService {

    private final ForumPostCommentRepository repo;
    private final ForumPostCommentLikeRepository likeRepo;
    private final ForumPostRepository postRepo;

    @Override
    public Page<ForumPostCommentRes> listByPost(UUID postId, Pageable pageable, UUID viewerId) {

        if (!postRepo.existsByIdAndDeletedAtIsNull(postId)) {
            throw new EntityNotFoundException("게시글을 찾을 수 없습니다: " + postId);
        }

        Page<ForumPostComment> page = repo.findByPostIdAndDeletedAtIsNull(postId, pageable);

        if (viewerId == null) {
            return page.map(ForumPostCommentMapper::toRes);
        }
        Set<UUID> ids = page.map(ForumPostComment::getId).toSet();
        Set<UUID> likedIds = ids.isEmpty() ? Set.of() : likeRepo.findLikedCommentIdsByUser(viewerId, ids);
        return page.map(c -> ForumPostCommentMapper.toRes(c, likedIds.contains(c.getId())));
    }

    @Override
    public Page<ForumPostCommentRes> listByUser(UUID userId, Pageable pageable, UUID viewerId) {
        Page<ForumPostComment> page = repo.findByCreatedByAndDeletedAtIsNull(userId, pageable);
        if (viewerId == null) {
            return page.map(ForumPostCommentMapper::toRes);
        }
        Set<UUID> ids = page.map(ForumPostComment::getId).toSet();
        Set<UUID> likedIds = ids.isEmpty() ? Set.of() : likeRepo.findLikedCommentIdsByUser(viewerId, ids);
        return page.map(c -> ForumPostCommentMapper.toRes(c, likedIds.contains(c.getId())));
    }

    @Override
    public ForumPostCommentRes get(UUID commentId, UUID viewerId) {
        ForumPostComment c = repo.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + commentId));
        boolean liked = (viewerId != null) && likeRepo.existsByCommentIdAndUserId(commentId, viewerId);
        return ForumPostCommentMapper.toRes(c, liked);
    }
}
