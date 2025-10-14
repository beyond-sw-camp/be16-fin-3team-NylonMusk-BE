package com.beyond.MKX.domain.forumPost.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(
        name = "forum_post",
        indexes = {
                @Index(name = "idx_post_active_created", columnList = "deleted_at, created_at DESC"),
                @Index(name = "idx_post_by_user_active_created", columnList = "created_by, deleted_at, created_at DESC"),
                @Index(name = "idx_post_by_stock_active_created", columnList = "stock_id, deleted_at, created_at DESC"),
                @Index(name = "idx_post_by_status_active_created", columnList = "status, deleted_at, created_at DESC")
        }
)
/**
 * 삭제는 실제 DELETE 대신 소프트 삭제로 처리하며,
 * 낙관적 락(@Version) 충돌 체크 및 버전 증가를 위해 커스텀 SQL에 version 조건/증가를 포함합니다.
 */
@SQLDelete(sql = """
UPDATE forum_post
SET deleted_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE id = ? AND version = ?
""")
@SQLRestriction("deleted_at IS NULL")
public class ForumPost extends BaseIdAndTimeEntity {
    @Column(name = "stock_id")
    @Comment("관련 종목 식별자(선택)")
    private UUID stockId;

    /** 작성자 식별자 (게이트웨이 X-User-Id 헤더와 일치) */
    @Column(name = "created_by", nullable = false)
    @Comment("작성자 사용자/관리자 UUID")
    private UUID createdBy;

    /** 제목 */
    @Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /** 본문 */
    @Column(name = "contents", nullable = false, columnDefinition = "TEXT")
    private String contents;

    /** 상태: CREATED/UPDATED/HIDE/PINNED */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    private PostStatus status = PostStatus.CREATED;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private int likesCount = 0;

    @Column(name = "comment_count", nullable = false)
    @Builder.Default
    private int commentCount = 0;

    /** 대표 이미지 URL */
    @Size(max = 512)
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    /** 낙관적 락 버전 */
    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /* ===================== 비즈니스 편의 메서드 ===================== */
    /** 내용/제목 수정 */
    public void updateContents(String newTitle, String newContents, String newImageUrl) {
        if (newTitle != null) this.title = newTitle;
        if (newContents != null) this.contents = newContents;
        if (newImageUrl != null) this.imageUrl = newImageUrl;
        this.status = PostStatus.UPDATED;
    }

    /** 숨김/해제 */
    public void hide()    { this.status = PostStatus.HIDE; }
    public void unhide()  { this.status = PostStatus.UPDATED; }

    /** 상단 고정/해제 (단일 상태 열거형 모델) */
    public void pin()     { this.status = PostStatus.PINNED; }
    public void unpin()   { this.status = PostStatus.UPDATED; }

    /** 좋아요/댓글 수 증감 (음수 방지) */
    public void incLikes()     { this.likesCount += 1; }
    public void decLikes()     { if (this.likesCount > 0) this.likesCount -= 1; }
    public void incComments()  { this.commentCount += 1; }
    public void decComments()  { if (this.commentCount > 0) this.commentCount -= 1; }

    /** 카테고리 연결은 조인 엔티티(ForumPostCategory)에서 관리 예정 */
    public void attachCategory(UUID categoryId) {
        // todo: ForumPostCategory 엔티티 도입 시 구현
    }
}
