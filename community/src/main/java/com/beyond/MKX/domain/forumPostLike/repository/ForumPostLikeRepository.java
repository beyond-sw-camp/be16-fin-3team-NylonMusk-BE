package com.beyond.MKX.domain.forumPostLike.repository;

import com.beyond.MKX.domain.forumPostLike.entity.ForumPostLike;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ForumPostLikeRepository extends JpaRepository<ForumPostLike, UUID> {
    @Query("SELECT l FROM ForumPostLike l WHERE l.post.id = :postId AND l.userId = :userId")
    Optional<ForumPostLike> findByPostIdAndUserId(@Param("postId") UUID postId, @Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM ForumPostLike l WHERE l.post.id = :postId AND l.userId = :userId")
    int deleteByPostIdAndUserId(@Param("postId") UUID postId, @Param("userId") UUID userId);

    boolean existsByPostIdAndUserId(@Param("postId") UUID postId, @Param("userId") UUID userId);

    @Query("""
           select l.post.id
           from ForumPostLike l
           where l.userId = :userId and l.post.id in :postIds
           """)
    Set<UUID> findLikedPostIdsByUser(
            @Param("userId") UUID userId,
            @Param("postIds") Collection<UUID> postIds
    );
}
