// com/beyond/MKX/domain/forumVote/service/query/ForumVoteQueryServiceImpl.java
package com.beyond.MKX.domain.forumVote.service.query;

import com.beyond.MKX.domain.forumVote.dto.*;
import com.beyond.MKX.domain.forumVote.entity.ForumVote;
import com.beyond.MKX.domain.forumVote.entity.ForumVoteSelection;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteLogRepository;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteRepository;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteSelectionRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumVoteQueryServiceImpl implements ForumVoteQueryService {

    private final ForumVoteRepository voteRepo;
    private final ForumVoteSelectionRepository selectionRepo;
    private final ForumVoteLogRepository logRepo;

    @Override
    public ForumVoteResDto get(UUID voteId, UUID viewerId) {
        ForumVote v = voteRepo.findById(voteId)
                .orElseThrow(() -> new EntityNotFoundException("ForumVote not found: " + voteId));

        List<ForumVoteSelection> sels = selectionRepo.findByVote_IdOrderBySortOrderAsc(voteId);

        boolean votedByMe = (viewerId != null) && logRepo.existsByVote_IdAndVotedBy(voteId, viewerId);

        return toResDto(v, sels, votedByMe);
    }

    @Override
    public ForumVoteResDto getByPost(UUID postId, UUID viewerId) {
        ForumVote v = voteRepo.findByForumPost_Id(postId)
                .orElseThrow(() -> new EntityNotFoundException("해당 게시글에는 투표가 존재하지 않습니다."));
        List<ForumVoteSelection> sels = selectionRepo.findByVote_IdOrderBySortOrderAsc(v.getId());
        boolean votedByMe = (viewerId != null) && logRepo.existsByVote_IdAndVotedBy(v.getId(), viewerId);
        return toResDto(v, sels, votedByMe);
    }

    private ForumVoteResDto toResDto(ForumVote v, List<ForumVoteSelection> sels, boolean votedByMe) {
        var selDtos = sels.stream()
                .sorted(Comparator.comparingInt(ForumVoteSelection::getSortOrder))
                .map(s -> ForumVoteSelectionResDto.builder()
                        .id(s.getId())
                        .text(s.getText())
                        .sortOrder(s.getSortOrder())
                        .votesCount(s.getVotesCount())
                        .build())
                .toList();

        return ForumVoteResDto.builder()
                .id(v.getId())
                .forumPostId(v.getForumPost().getId())
                .title(v.getTitle())
                .allowMultipleCount(v.getAllowMultipleCount())
                .totalVoters(v.getTotalVoters())
                .votedByMe(votedByMe)
                .selections(selDtos)
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .version(v.getVersion())
                .build();
    }
}
