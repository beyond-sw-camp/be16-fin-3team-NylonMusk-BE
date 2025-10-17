package com.beyond.MKX.domain.forumPostLike.entity;

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
        name = "forum_post_like",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_forum_post_like", columnNames = {"post_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_fpl_post_id", columnList = "post_id"),
                @Index(name = "idx_fpl_user_id", columnList = "user_id")
        }
)
public class ForumPostLike extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fpl_post"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ForumPost post;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;
}
