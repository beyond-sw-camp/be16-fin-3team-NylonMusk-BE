package com.beyond.MKX.domain.forumVote.service.admin;

import com.beyond.MKX.domain.forumVote.dto.ForumVoteResDto;
import com.beyond.MKX.domain.forumVote.dto.ForumVoteSelectionResDto;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteRepository;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteSelectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumVoteAdminServiceImpl implements ForumVoteAdminService {

    private final ForumVoteRepository voteRepo;
    private final ForumVoteSelectionRepository selectionRepo;

    @Override
    public Page<ForumVoteResDto> listAll(Pageable pageable) {
        return voteRepo.findAll(pageable)
                .map(v -> ForumVoteResDto.builder()
                        .id(v.getId())
                        .forumPostId(v.getForumPost().getId())
                        .title(v.getTitle())
                        .allowMultipleCount(v.getAllowMultipleCount())
                        .totalVoters(v.getTotalVoters())
                        .votedByMe(false)
                        .selections(selectionRepo.findByVote_IdOrderBySortOrderAsc(v.getId()).stream()
                                .map(s -> ForumVoteSelectionResDto.builder()
                                        .id(s.getId())
                                        .text(s.getText())
                                        .sortOrder(s.getSortOrder())
                                        .votesCount(s.getVotesCount())
                                        .build())
                                .toList())
                        .createdAt(v.getCreatedAt())
                        .updatedAt(v.getUpdatedAt())
                        .version(v.getVersion())
                        .build());
    }
}
