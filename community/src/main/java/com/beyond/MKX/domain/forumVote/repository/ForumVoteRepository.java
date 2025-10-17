package com.beyond.MKX.domain.forumVote.repository;

import com.beyond.MKX.domain.forumVote.entity.ForumVote;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ForumVoteRepository extends JpaRepository<ForumVote, UUID> {
    Optional<ForumVote> findByForumPost_Id(UUID forumPostId);
    boolean existsByForumPost_Id(UUID forumPostId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from ForumVote v where v.id = :id")
    Optional<ForumVote> findByIdForUpdate(@Param("id") UUID id);
}
