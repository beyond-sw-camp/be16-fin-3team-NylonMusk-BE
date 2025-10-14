package com.beyond.MKX.domain.forumCategory.entity;

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
        name = "forum_category",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_forum_category_name", columnNames = "name")
        },
        indexes = {
                @Index(name = "idx_forum_category_created_at", columnList = "created_at"),
                @Index(name = "idx_forum_category_deleted_at", columnList = "deleted_at")
        }
)

@SQLDelete(sql = """
UPDATE forum_category
SET deleted_at = CURRENT_TIMESTAMP,
    version = version + 1
WHERE id = ? AND version = ?
""")
@SQLRestriction("deleted_at IS NULL")
public class ForumCategory extends BaseIdAndTimeEntity {

    @Size(max = 25) // 실무에선 DTO에서 검증 권장
    @Column(name = "name", nullable = false, length = 25)
    private String name;

    @Size(max = 255)
    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "created_by", nullable = false, updatable = false)
    @Comment("관리자 아이디")
    private UUID createdBy;

    /** 동시성 제어(낙관적 락) - 선택이지만 권장 */
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
