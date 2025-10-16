package com.beyond.MKX.domain.forumPostComment.repository;

import com.beyond.MKX.domain.forumPostComment.entity.ForumPostComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ForumPostCommentRepository extends JpaRepository<ForumPostComment, UUID> {

    Page<ForumPostComment> findByPostIdAndDeletedAtIsNull(UUID postId, Pageable pageable);
    Page<ForumPostComment> findByCreatedByAndDeletedAtIsNull(UUID createdBy, Pageable pageable);
    Optional<ForumPostComment> findByIdAndDeletedAtIsNull(UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ForumPostComment c SET c.likes = c.likes + 1 WHERE c.id = :id AND c.deletedAt IS NULL")
    int incrementLikes(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ForumPostComment c SET c.likes = CASE WHEN c.likes > 0 THEN c.likes - 1 ELSE 0 END " +
            "WHERE c.id = :id AND c.deletedAt IS NULL")
    int decrementLikes(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE ForumPostComment c SET c.deletedAt = :now WHERE c.id = :id AND c.deletedAt IS NULL")
    int softDelete(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
