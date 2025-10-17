package com.beyond.MKX.domain.forumVote.repository;

import com.beyond.MKX.domain.forumVote.entity.ForumVoteSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ForumVoteSelectionRepository extends JpaRepository<ForumVoteSelection, UUID> {
    List<ForumVoteSelection> findByVote_IdOrderBySortOrderAsc(UUID voteId);
    List<ForumVoteSelection> findByIdIn(Iterable<UUID> ids);

    @Modifying
    @Query("""
       update ForumVoteSelection s
       set s.sortOrder = s.sortOrder + :offset
       where s.vote.id = :voteId
       """)
    int bumpSortOrders(@Param("voteId") UUID voteId, @Param("offset") int offset);
}
