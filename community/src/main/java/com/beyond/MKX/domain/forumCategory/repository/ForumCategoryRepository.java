package com.beyond.MKX.domain.forumCategory.repository;

import com.beyond.MKX.domain.forumCategory.entity.ForumCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ForumCategoryRepository extends JpaRepository<ForumCategory, UUID> {

    /** 단건(삭제 포함) — Command 경로에서 복구/재삭제 등을 위해 필요 */
    @Query(value = "SELECT * FROM forum_category WHERE id = :id", nativeQuery = true)
    Optional<ForumCategory> findOneIncludingDeleted(@Param("id") UUID id);

    /** 활성(미삭제)만 — 수정 시 보수적으로 사용하고 싶다면 활용 */
    @Query(value = "SELECT * FROM forum_category WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    Optional<ForumCategory> findActiveById(@Param("id") UUID id);

    /** 이름 중복 검사 (수정/생성시 사용 가능) */
    boolean existsByName(String name);
}
