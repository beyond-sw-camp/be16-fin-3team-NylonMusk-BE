package com.beyond.MKX.domain.forumVote.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "forum_vote",
        indexes = {
                @Index(name = "idx_forum_vote_post_id", columnList = "forum_post_id")
        }
)
public class ForumVote extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "forum_post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_forum_vote_post"))
    private ForumPost forumPost;

    @Column(nullable = false, length = 100)
    private String title;

    @Min(1)
    @Column(name = "allow_multiple_count", nullable = false)
    private int allowMultipleCount = 1;

    /** 투표에 참여한 distinct 사용자 수 */
    @Column(name = "total_voters", nullable = false)
    private int totalVoters = 0;

    @Version
    private long version;

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<ForumVoteSelection> selections = new ArrayList<>();

    public void incVoter() { this.totalVoters++; }
    public void decVoter() { if (this.totalVoters > 0) this.totalVoters--; }
}
