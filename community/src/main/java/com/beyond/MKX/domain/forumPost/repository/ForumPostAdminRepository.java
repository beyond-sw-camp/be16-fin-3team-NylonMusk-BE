// com/beyond/MKX/domain/forumPost/repository/ForumPostAdminRepository.java
package com.beyond.MKX.domain.forumPost.repository;

import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ForumPostAdminRepository extends JpaRepository<ForumPost, UUID> {

    // 삭제 포함 전체
    @Query(value = "select * from forum_post order by created_at desc",
            countQuery = "select count(*) from forum_post",
            nativeQuery = true)
    Page<ForumPost> findAllIncludingDeleted(Pageable pageable);

    // 삭제만
    @Query(value = "select * from forum_post where deleted_at is not null order by deleted_at desc",
            countQuery = "select count(*) from forum_post where deleted_at is not null",
            nativeQuery = true)
    Page<ForumPost> findAllDeleted(Pageable pageable);

    // 단건(삭제 포함)
    @Query(value = "select * from forum_post where id = :id limit 1", nativeQuery = true)
    Optional<ForumPost> findByIdIncludingDeleted(@Param("id") UUID id);

    // 복구 (deleted_at NULL로, version + 1)
    @Modifying
    @Query(value = "update forum_post set deleted_at = null, version = version + 1 where id = :id", nativeQuery = true)
    int restoreById(@Param("id") UUID id);

    // 조인 테이블 먼저 삭제 (FK 제약 보호)
    @Modifying
    @Query(value = "delete from forum_post_category where post_id = :id", nativeQuery = true)
    int deleteCategoriesByPostId(@Param("id") UUID id);

    // 완전 삭제 (소프트 삭제 우회)
    @Modifying
    @Query(value = "delete from forum_post where id = :id", nativeQuery = true)
    int hardDeleteById(@Param("id") UUID id);
}
