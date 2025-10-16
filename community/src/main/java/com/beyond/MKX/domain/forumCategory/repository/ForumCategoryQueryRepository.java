package com.beyond.MKX.domain.forumCategory.repository;

import com.beyond.MKX.domain.forumCategory.entity.ForumCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * 조회 전용 레포지토리 (삭제 포함/전용/미삭제, 작성자 기준 등)
 * - @SQLRestriction("deleted_at IS NULL")이 엔티티에 걸려 있어도,
 *   아래 네이티브 쿼리는 해당 제약의 영향을 받지 않습니다.
 */
public interface ForumCategoryQueryRepository extends JpaRepository<ForumCategory, UUID> {

    /* 단건(삭제 포함) */
    @Query(value = "SELECT * FROM forum_category WHERE id = :id", nativeQuery = true)
    Optional<ForumCategory> findOneIncludingDeleted(@Param("id") UUID id);

    /* 전체(삭제 포함) */
    @Query(value = "SELECT * FROM forum_category ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category",
            nativeQuery = true)
    Page<ForumCategory> findAllIncludingDeleted(Pageable pageable);

    /* 삭제 전용 */
    @Query(value = "SELECT * FROM forum_category WHERE deleted_at IS NOT NULL ORDER BY deleted_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE deleted_at IS NOT NULL",
            nativeQuery = true)
    Page<ForumCategory> findDeletedOnly(Pageable pageable);

    /* 미삭제(활성) 전용 */
    @Query(value = "SELECT * FROM forum_category WHERE deleted_at IS NULL ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE deleted_at IS NULL",
            nativeQuery = true)
    Page<ForumCategory> findActiveOnly(Pageable pageable);

    /* 특정 사용자(UUID) */
    @Query(value = "SELECT * FROM forum_category WHERE created_by = :uid ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE created_by = :uid",
            nativeQuery = true)
    Page<ForumCategory> findAllByCreator(@Param("uid") UUID userUuid, Pageable pageable);

    @Query(value = "SELECT * FROM forum_category WHERE created_by = :uid AND deleted_at IS NULL ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE created_by = :uid AND deleted_at IS NULL",
            nativeQuery = true)
    Page<ForumCategory> findActiveByCreator(@Param("uid") UUID userUuid, Pageable pageable);

    @Query(value = "SELECT * FROM forum_category WHERE created_by = :uid AND deleted_at IS NOT NULL ORDER BY deleted_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE created_by = :uid AND deleted_at IS NOT NULL",
            nativeQuery = true)
    Page<ForumCategory> findDeletedByCreator(@Param("uid") UUID userUuid, Pageable pageable);

    /* 특정 사용자(UserId 문자열) — created_by_user_id 컬럼이 있는 경우 */
    @Query(value = "SELECT * FROM forum_category WHERE created_by_user_id = :userId ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE created_by_user_id = :userId",
            nativeQuery = true)
    Page<ForumCategory> findAllByCreatorUserId(@Param("userId") String userId, Pageable pageable);

    @Query(value = "SELECT * FROM forum_category WHERE created_by_user_id = :userId AND deleted_at IS NULL ORDER BY created_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE created_by_user_id = :userId AND deleted_at IS NULL",
            nativeQuery = true)
    Page<ForumCategory> findActiveByCreatorUserId(@Param("userId") String userId, Pageable pageable);

    @Query(value = "SELECT * FROM forum_category WHERE created_by_user_id = :userId AND deleted_at IS NOT NULL ORDER BY deleted_at DESC",
            countQuery = "SELECT COUNT(*) FROM forum_category WHERE created_by_user_id = :userId AND deleted_at IS NOT NULL",
            nativeQuery = true)
    Page<ForumCategory> findDeletedByCreatorUserId(@Param("userId") String userId, Pageable pageable);
}
