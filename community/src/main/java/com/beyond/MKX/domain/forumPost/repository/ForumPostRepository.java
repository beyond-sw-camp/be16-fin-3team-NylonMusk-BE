package com.beyond.MKX.domain.forumPost.repository;

import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 엔티티에 @SQLRestriction("deleted_at IS NULL")가 걸려 있으므로
 * 아래 메서드들은 기본적으로 "미삭제" 레코드만 반환합니다.
 */
public interface ForumPostRepository extends JpaRepository<ForumPost, UUID> {

    // 전체(미삭제), 최신순: Pageable에 sort=createdAt,desc 전달 권장
    Page<ForumPost> findAllBy(Pageable pageable);

    // 상태 필터
    Page<ForumPost> findByStatus(PostStatus status, Pageable pageable);

    // 특정 사용자 글
    Page<ForumPost> findByCreatedBy(UUID createdBy, Pageable pageable);

    // 특정 사용자 + 상태
    Page<ForumPost> findByCreatedByAndStatus(UUID createdBy, PostStatus status, Pageable pageable);

    boolean existsByIdAndDeletedAtIsNull(UUID id);
}
