package com.beyond.MKX.domain.forumVote.repository;

import com.beyond.MKX.domain.forumVote.entity.ForumVoteLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface ForumVoteLogRepository extends JpaRepository<ForumVoteLog, UUID> {

    List<ForumVoteLog> findByVote_IdAndVotedBy(UUID voteId, UUID votedBy);

    boolean existsByVote_IdAndVotedBy(UUID voteId, UUID votedBy);

    @Modifying
    int deleteByVote_IdAndVotedBy(UUID voteId, UUID votedBy);
}
