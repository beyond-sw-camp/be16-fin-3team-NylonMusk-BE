package com.beyond.MKX.domain.forumPost.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.forumCategory.entity.ForumCategory;
import com.beyond.MKX.domain.common.entity.WriterRole;
import com.beyond.MKX.domain.forumVote.entity.ForumVote;
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

    @Column(name = "created_by", nullable = false)
    @Comment("작성자 사용자/관리자 UUID")
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "writer_role", nullable = false, length = 16)
    @Builder.Default
    private WriterRole writerRole = WriterRole.MEMBER;

    @Size(max = 255)
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "contents", nullable = false, columnDefinition = "TEXT")
    private String contents;

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

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.Set<ForumPostCategory> categories = new java.util.HashSet<>();

    @Size(max = 512)
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @OneToOne(mappedBy = "forumPost", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ForumVote vote;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public void updateContents(String newTitle, String newContents, String newImageUrl) {
        if (newTitle != null) this.title = newTitle;
        if (newContents != null) this.contents = newContents;
        if (newImageUrl != null) this.imageUrl = newImageUrl;
        this.status = PostStatus.UPDATED;
    }

    public void hide()    { this.status = PostStatus.HIDE; }
    public void unhide()  { this.status = PostStatus.UPDATED; }
    public void pin()     { this.status = PostStatus.PINNED; }
    public void unpin()   { this.status = PostStatus.UPDATED; }

    public void incLikes()     { this.likesCount += 1; }
    public void decLikes()     { if (this.likesCount > 0) this.likesCount -= 1; }
    public void incComments()  { this.commentCount += 1; }
    public void decComments()  { if (this.commentCount > 0) this.commentCount -= 1; }

    public void replaceCategories(java.util.Collection<ForumCategory> newCats) {
        if (this.categories == null) {
            this.categories = new java.util.HashSet<>();
        } else {
            this.categories.clear();
        }
        for (ForumCategory c : newCats) {
            this.categories.add(ForumPostCategory.builder()
                    .post(this)
                    .category(c)
                    .build());
        }
    }
}
