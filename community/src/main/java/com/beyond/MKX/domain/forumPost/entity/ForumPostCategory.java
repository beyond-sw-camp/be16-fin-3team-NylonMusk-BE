package com.beyond.MKX.domain.forumPost.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.forumCategory.entity.ForumCategory;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "forum_post_category",
        uniqueConstraints = @UniqueConstraint(name="uq_post_category", columnNames = {"post_id","category_id"}),
        indexes = {
                @Index(name="idx_fpc_post", columnList = "post_id"),
                @Index(name="idx_fpc_category", columnList = "category_id")
        })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class ForumPostCategory extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_fpc_post"))
    private ForumPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false,
            foreignKey = @ForeignKey(name="fk_fpc_category"))
    private ForumCategory category;
}
