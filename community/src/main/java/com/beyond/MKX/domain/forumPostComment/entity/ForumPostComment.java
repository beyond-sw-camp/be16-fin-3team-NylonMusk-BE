package com.beyond.MKX.domain.forumPostComment.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "forum_post_comment",
        indexes = {
                @Index(name = "idx_forum_post_comment_post_id", columnList = "post_id"),
                @Index(name = "idx_forum_post_comment_created_by", columnList = "created_by")
        }
)
public class ForumPostComment extends BaseIdAndTimeEntity {

    /** --- 연관관계(FK) --- */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            updatable = false,
            foreignKey = @ForeignKey(name = "fk_forum_post_comment_post")
    )
    @OnDelete(action = OnDeleteAction.CASCADE) // DB FK에 ON DELETE CASCADE도 함께 권장
    private ForumPost post;

    /** 읽기 전용 postId 미러(조인 없이 ID만 필요할 때 사용) */
    @Column(name = "post_id", insertable = false, updatable = false)
    private UUID postId;

    /** 작성자 아이디(관리자/사용자) — 추후 User 엔티티 FK로 바꿀 여지 */
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** 내용 (TEXT) */
    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /** 좋아요 수 (기본 0) */
    @Builder.Default
    @Column(name = "likes", nullable = false)
    private Integer likes = 0;

    /** 좋아요 +1 */
    public void incrementLike() {
        this.likes = (this.likes == null ? 0 : this.likes) + 1;
    }

    /** 좋아요 -1 (0 미만 방지) */
    public void decrementLike() {
        int cur = (this.likes == null ? 0 : this.likes);
        this.likes = Math.max(0, cur - 1);
    }
}
