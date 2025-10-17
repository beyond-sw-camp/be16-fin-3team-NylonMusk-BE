package com.beyond.MKX.domain.forumVote.service.command;

import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import com.beyond.MKX.domain.forumVote.dto.*;
import com.beyond.MKX.domain.forumVote.entity.ForumVote;
import com.beyond.MKX.domain.forumVote.entity.ForumVoteLog;
import com.beyond.MKX.domain.forumVote.entity.ForumVoteSelection;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteLogRepository;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteRepository;
import com.beyond.MKX.domain.forumVote.repository.ForumVoteSelectionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class ForumVoteCommandServiceImpl implements ForumVoteCommandService {

    private final ForumVoteRepository voteRepo;
    private final ForumVoteSelectionRepository selectionRepo;
    private final ForumVoteLogRepository logRepo;
    private final ForumPostRepository postRepo;

    @PersistenceContext
    private final EntityManager em;

    private boolean isAdmin(String role) {
        return role != null && role.toUpperCase(java.util.Locale.ROOT).contains("EXCHANGE");
    }

    @Override
    public ForumVoteResDto create(ForumVoteCreateReqDto req) {
        if (req.selections() == null || req.selections().isEmpty())
            throw new IllegalArgumentException("선택지는 최소 1개 이상이어야 합니다.");

        if (voteRepo.existsByForumPost_Id(req.forumPostId()))
            throw new IllegalStateException("이미 해당 게시글에 투표가 존재합니다.");

        ForumPost post = postRepo.findById(req.forumPostId())
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + req.forumPostId()));

        ForumVote vote = ForumVote.builder()
                .forumPost(post)
                .title(req.title())
                .allowMultipleCount(req.allowMultipleCount())
                .totalVoters(0)
                .build();

        voteRepo.save(vote);

        int order = 0;
        List<ForumVoteSelection> selections = new ArrayList<>();
        for (String text : req.selections()) {
            selections.add(ForumVoteSelection.builder()
                    .vote(vote)
                    .text(text)
                    .sortOrder(order++)
                    .votesCount(0)
                    .build());
        }
        selectionRepo.saveAll(selections);

        return toResDto(vote, selections, false);
    }

    @Override
    public ForumVoteCastResDto cast(UUID voteId, UUID actorId, ForumVoteCastReqDto req) {
        // 1) 입력 정규화: 중복 제거
        List<UUID> requested = req.selectionIds() == null ? List.of()
                : req.selectionIds().stream().distinct().toList();
        if (requested.isEmpty())
            throw new IllegalArgumentException("최소 1개 이상의 선택지를 선택해야 합니다.");

        // 2) 투표 행 잠금 (경합 제어)
        ForumVote vote = voteRepo.findByIdForUpdate(voteId)
                .orElseThrow(() -> new EntityNotFoundException("ForumVote not found: " + voteId));

        if (requested.size() > vote.getAllowMultipleCount())
            throw new IllegalArgumentException("허용 개수(" + vote.getAllowMultipleCount() + ")를 초과했습니다.");

        // 3) 선택지 유효성 확인
        List<ForumVoteSelection> selected = selectionRepo.findByIdIn(requested);
        if (selected.size() != requested.size())
            throw new IllegalArgumentException("유효하지 않은 선택지 ID가 포함되어 있습니다.");
        for (ForumVoteSelection s : selected) {
            if (!s.getVote().getId().equals(voteId))
                throw new IllegalArgumentException("다른 투표의 선택지가 포함되어 있습니다.");
        }

        // 4) 기존 내 표 조회
        List<ForumVoteLog> oldLogs = logRepo.findByVote_IdAndVotedBy(voteId, actorId);
        Set<UUID> oldSelIds = oldLogs.stream().map(l -> l.getSelection().getId()).collect(java.util.stream.Collectors.toSet());
        Set<UUID> newSelIds = new java.util.HashSet<>(requested);

        // 4-1) 완전 동일하면 No-Op (중복 커밋 방지)
        if (oldSelIds.equals(newSelIds)) {
            return ForumVoteCastResDto.builder()
                    .voteId(voteId)
                    .totalVoters(vote.getTotalVoters())
                    .mySelections(requested)
                    .build();
        }

        // 5) 변경 적용
        applyReplaceBallots(vote, actorId, oldLogs, selected);

        return ForumVoteCastResDto.builder()
                .voteId(voteId)
                .totalVoters(vote.getTotalVoters())
                .mySelections(requested)
                .build();
    }

    /** 기존 표를 삭제하고 새 표로 교체 (flush 순서 보장 + 1회 재시도 포함) */
    private void applyReplaceBallots(ForumVote vote,
                                     UUID actorId,
                                     List<ForumVoteLog> oldLogs,
                                     List<ForumVoteSelection> newSelections) {

        boolean hadVotes = !oldLogs.isEmpty();

        // A) 이전 표 제거(집계 감소) + bulk delete + 즉시 flush
        if (hadVotes) {
            for (ForumVoteLog l : oldLogs) {
                ForumVoteSelection sel = l.getSelection();
                sel.dec();
                selectionRepo.save(sel);
            }
            logRepo.deleteByVote_IdAndVotedBy(vote.getId(), actorId);
            em.flush(); // delete가 DB에 반영된 후 insert가 나가도록 보장
            // totalVoters는 아래에서 다시 증가시키므로 우선 감소
            vote.decVoter();
        }

        // B) 새 표 insert + 집계 증가 (예외 시 1회 재시도)
        try {
            for (ForumVoteSelection sel : newSelections) {
                logRepo.save(ForumVoteLog.builder()
                        .vote(vote)
                        .selection(sel)
                        .votedBy(actorId)
                        .build());
                sel.inc();
                selectionRepo.save(sel);
            }
            vote.incVoter();
            voteRepo.save(vote);
            em.flush(); // insert와 집계 반영 flush

        } catch (DataIntegrityViolationException e) {
            // 동시성 등으로 중복 키가 발생한 경우: 강제 정리 후 1회 재시도 (Last-write-wins)
            em.clear();
            // 안전하게 한 번 더 클린업
            logRepo.deleteByVote_IdAndVotedBy(vote.getId(), actorId);
            em.flush();

            // 재시도
            for (ForumVoteSelection sel : newSelections) {
                logRepo.save(ForumVoteLog.builder()
                        .vote(vote)
                        .selection(sel)
                        .votedBy(actorId)
                        .build());
                sel.inc();
                selectionRepo.save(sel);
            }
            // 재시도 시, 사용자가 최종적으로 표를 가진 상태이므로 보수적으로 보정
            // (위에서 hadVotes이면 decVoter()가 실행되었으니 여기서 incVoter()는 필요)
            vote.incVoter();
            voteRepo.save(vote);
            em.flush();
        }
    }

    @Override
    public ForumVoteCastResDto revoke(UUID voteId, UUID actorId) {
        ForumVote vote = voteRepo.findById(voteId)
                .orElseThrow(() -> new EntityNotFoundException("ForumVote not found: " + voteId));

        List<ForumVoteLog> oldLogs = logRepo.findByVote_IdAndVotedBy(voteId, actorId);
        if (oldLogs.isEmpty()) {
            return ForumVoteCastResDto.builder()
                    .voteId(voteId)
                    .totalVoters(vote.getTotalVoters())
                    .mySelections(List.of())
                    .build();
        }

        for (ForumVoteLog l : oldLogs) {
            ForumVoteSelection sel = l.getSelection();
            sel.dec();
            selectionRepo.save(sel);
        }
        logRepo.deleteByVote_IdAndVotedBy(voteId, actorId);
        vote.decVoter();
        voteRepo.save(vote);

        return ForumVoteCastResDto.builder()
                .voteId(voteId)
                .totalVoters(vote.getTotalVoters())
                .mySelections(List.of())
                .build();
    }

    @Override
    @Transactional
    public void reorderSelections(UUID voteId, UUID actorId, String actorRole, List<UUID> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new IllegalArgumentException("selectionIdsInOrder는 비어 있을 수 없습니다.");
        }

        // 1) 투표 행 락 + 소유자 권한 확인
        var vote = voteRepo.findByIdForUpdate(voteId)
                .orElseThrow(() -> new EntityNotFoundException("ForumVote not found: " + voteId));

        UUID ownerId = vote.getForumPost().getCreatedBy(); // LAZY여도 트랜잭션 내 접근 OK
        if (!isAdmin(actorRole) && !ownerId.equals(actorId)) {
            throw new SecurityException("선택지 순서를 변경할 권한이 없습니다. (작성자 또는 관리자만 가능)");
        }

        // 2) 현재 선택지 목록
        var selections = selectionRepo.findByVote_IdOrderBySortOrderAsc(voteId);
        if (selections.size() != orderedIds.size()) {
            throw new IllegalArgumentException("개수 불일치: 현재 선택지 수와 전달된 ID 수가 다릅니다.");
        }

        // 3) 집합/중복 검증
        var currentIds = selections.stream().map(ForumVoteSelection::getId).collect(java.util.stream.Collectors.toSet());
        var incomingOrdered = new java.util.LinkedHashSet<>(orderedIds); // 순서 유지 + 중복 제거
        if (incomingOrdered.size() != orderedIds.size()) {
            throw new IllegalArgumentException("중복 ID가 포함되어 있습니다.");
        }
        if (!currentIds.equals(incomingOrdered)) {
            throw new IllegalArgumentException("선택지 집합이 일치하지 않습니다.");
        }

        // 4) 동일 순서면 No-Op
        boolean sameOrder = true;
        for (int i = 0; i < selections.size(); i++) {
            if (!selections.get(i).getId().equals(orderedIds.get(i))) { sameOrder = false; break; }
        }
        if (sameOrder) return;

        // 5) 유니크 충돌 방지용 2단계 업데이트
        final int OFFSET = 100_000;
        selectionRepo.bumpSortOrders(voteId, OFFSET);
        em.flush(); // 임시 값 반영

        var byId = selections.stream().collect(java.util.stream.Collectors.toMap(ForumVoteSelection::getId, x -> x));
        int idx = 0;
        for (UUID id : orderedIds) {
            byId.get(id).setSortOrder(idx++);
        }
        selectionRepo.saveAll(selections);
        em.flush();
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
