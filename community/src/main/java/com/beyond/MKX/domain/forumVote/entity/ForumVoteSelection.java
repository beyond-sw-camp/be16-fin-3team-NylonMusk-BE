package com.beyond.MKX.domain.forumVote.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "forum_vote_selection",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_vote_selection_order", columnNames = {"vote_id","sort_order"})
        },
        indexes = {
                @Index(name = "idx_vote_selection_vote", columnList = "vote_id"),
                @Index(name = "idx_vote_selection_order", columnList = "vote_id, sort_order")
        }
)
public class ForumVoteSelection extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_selection_vote"))
    private ForumVote vote;

    @Column(name = "text", nullable = false, length = 200)
    private String text;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 해당 선택지의 총 표 수(집계 컬럼) */
    @Column(name = "votes_count", nullable = false)
    private int votesCount = 0;

    public void inc() { this.votesCount++; }
    public void dec() { if (this.votesCount > 0) this.votesCount--; }
}
