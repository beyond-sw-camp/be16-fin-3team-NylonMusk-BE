package com.beyond.MKX.domain.forumPostLike.service;

import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import com.beyond.MKX.domain.forumPostLike.dto.PostLikeToggleRes;
import com.beyond.MKX.domain.forumPostLike.entity.ForumPostLike;
import com.beyond.MKX.domain.forumPostLike.repository.ForumPostLikeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 게시글 좋아요 토글 서비스 구현
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ForumPostLikeService {

    private final ForumPostRepository postRepo;
    private final ForumPostLikeRepository likeRepo;

    /**
     * 이미 좋아요면 삭제, 아니면 생성.
     * ForumPost.likesCount 도 동기화.
     */
    public PostLikeToggleRes toggleLike(UUID userId, UUID postId) {
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));

        boolean exists = likeRepo.existsByPostIdAndUserId(postId, userId);

        if (exists) {
            likeRepo.deleteByPostIdAndUserId(postId, userId);
            post.decLikes();
            postRepo.save(post);
            return PostLikeToggleRes.builder()
                    .liked(false)
                    .likeCount(post.getLikesCount())
                    .build();
        } else {
            ForumPostLike like = ForumPostLike.builder()
                    .post(post)
                    .userId(userId)
                    .build();
            likeRepo.save(like);
            post.incLikes();
            postRepo.save(post);
            return PostLikeToggleRes.builder()
                    .liked(true)
                    .likeCount(post.getLikesCount())
                    .build();
        }
    }
}
