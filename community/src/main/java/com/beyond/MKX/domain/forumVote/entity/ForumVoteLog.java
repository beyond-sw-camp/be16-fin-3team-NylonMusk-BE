package com.beyond.MKX.domain.forumVote.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "forum_vote_log",
        uniqueConstraints = {
                // 동일 사용자-투표-선택지 중복 방지
                @UniqueConstraint(name = "uq_vote_log", columnNames = {"vote_id","selection_id","voted_by"})
        },
        indexes = {
                @Index(name = "idx_vote_log_vote", columnList = "vote_id"),
                @Index(name = "idx_vote_log_user", columnList = "voted_by")
        }
)
public class ForumVoteLog extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vote_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_log_vote"))
    private ForumVote vote;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "selection_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_log_selection"))
    private ForumVoteSelection selection;

    @Column(name = "voted_by", nullable = false, updatable = false)
    private UUID votedBy;
}
