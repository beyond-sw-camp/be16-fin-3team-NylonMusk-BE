package com.beyond.MKX.domain.forumPostComment.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
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
        name = "forum_post_comment_like",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_forum_post_comment_like", columnNames = {"comment_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_fpcl_comment_id", columnList = "comment_id"),
                @Index(name = "idx_fpcl_user_id", columnList = "user_id")
        }
)
public class ForumPostCommentLike extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fpcl_comment"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ForumPostComment comment;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
}
