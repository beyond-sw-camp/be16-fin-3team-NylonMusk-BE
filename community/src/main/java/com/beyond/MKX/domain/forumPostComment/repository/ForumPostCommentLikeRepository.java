package com.beyond.MKX.domain.forumPostComment.repository;

import com.beyond.MKX.domain.forumPostComment.entity.ForumPostCommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ForumPostCommentLikeRepository extends JpaRepository<ForumPostCommentLike, UUID> {

    @Query("SELECT l FROM ForumPostCommentLike l WHERE l.comment.id = :commentId AND l.userId = :userId")
    Optional<ForumPostCommentLike> findByCommentIdAndUserId(UUID commentId, UUID userId);

    @Modifying
    @Query("DELETE FROM ForumPostCommentLike l WHERE l.comment.id = :commentId AND l.userId = :userId")
    int deleteByCommentIdAndUserId(UUID commentId, UUID userId);

    boolean existsByCommentIdAndUserId(UUID commentId, UUID userId);

    @Query("""
           select l.comment.id
           from ForumPostCommentLike l
           where l.userId = :userId and l.comment.id in :commentIds
           """)
    Set<UUID> findLikedCommentIdsByUser(
            @Param("userId") UUID userId,
            @Param("commentIds") Collection<UUID> commentIds
    );
}
