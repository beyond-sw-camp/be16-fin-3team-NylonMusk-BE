package com.beyond.MKX.domain.forumPost.service.query;

import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.entity.ForumPostCategory;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import com.beyond.MKX.domain.forumPostLike.repository.ForumPostLikeRepository;
import com.beyond.MKX.domain.forumPostComment.service.ForumPostCommentQueryService;
import com.beyond.MKX.domain.forumVote.service.query.ForumVoteQueryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumPostQueryServiceImpl implements ForumPostQueryService {

    private final ForumPostRepository postRepo;
    private final ForumPostLikeRepository likeRepo;
    private final ForumPostCommentQueryService commentQueryService;
    private final ForumVoteQueryService voteQueryService;

    @Override
    public Page<ForumPostResDto> list(PostStatus status, Pageable pageable, UUID viewerId) {
        Page<ForumPost> page = (status == null)
                ? postRepo.findAllBy(pageable)
                : postRepo.findByStatus(status, pageable);

        Set<UUID> likedPostIds = Collections.emptySet();
        if (viewerId != null && !page.isEmpty()) {
            List<UUID> ids = page.map(ForumPost::getId).toList();
            likedPostIds = likeRepo.findLikedPostIdsByUser(viewerId, ids);
        }

        Set<UUID> finalLiked = likedPostIds;
        return page.map(p -> map(p, finalLiked.contains(p.getId()), viewerId));
    }

    @Override
    public Page<ForumPostResDto> listMine(UUID me, PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? postRepo.findByCreatedBy(me, pageable)
                : postRepo.findByCreatedByAndStatus(me, status, pageable);
        return page.map(p -> map(p, false, null));
    }

    @Override
    public Page<ForumPostResDto> listByUser(UUID userId, PostStatus status, Pageable pageable, UUID viewerId) {
        Page<ForumPost> page = (status == null)
                ? postRepo.findByCreatedBy(userId, pageable)
                : postRepo.findByCreatedByAndStatus(userId, status, pageable);

        Set<UUID> likedPostIds = Collections.emptySet();
        if (viewerId != null && !page.isEmpty()) {
            List<UUID> ids = page.map(ForumPost::getId).toList();
            likedPostIds = likeRepo.findLikedPostIdsByUser(viewerId, ids);
        }

        Set<UUID> finalLiked = likedPostIds;
        return page.map(p -> map(p, finalLiked.contains(p.getId()), viewerId));
    }

    @Override
    public Page<ForumPostResDto> listByStock(UUID stockId, PostStatus status, Pageable pageable, UUID viewerId) {
        Page<ForumPost> page = (status == null)
                ? postRepo.findByStockId(stockId, pageable)
                : postRepo.findByStockIdAndStatus(stockId, status, pageable);

        Set<UUID> likedPostIds = Collections.emptySet();
        if (viewerId != null && !page.isEmpty()) {
            List<UUID> ids = page.map(ForumPost::getId).toList();
            likedPostIds = likeRepo.findLikedPostIdsByUser(viewerId, ids);
        }

        Set<UUID> finalLiked = likedPostIds;
        return page.map(p -> map(p, finalLiked.contains(p.getId()), viewerId));
    }

    @Override
    public ForumPostResDto get(UUID postId, UUID viewerId) {
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));
        boolean liked = viewerId != null && likeRepo.existsByPostIdAndUserId(postId, viewerId);
        return map(post, liked, viewerId);
    }

    @Override
    public ForumPostResDto getWithDetails(UUID postId, UUID viewerId) {
        ForumPost post = postRepo.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + postId));
        boolean liked = viewerId != null && likeRepo.existsByPostIdAndUserId(postId, viewerId);
        return mapWithDetails(post, liked, viewerId);
    }

    private ForumPostResDto map(ForumPost p, boolean likedByMe, UUID viewerId) {
        List<ForumCategoryResDto> catDtos = p.getCategories().stream()
                .map(ForumPostCategory::getCategory)
                .map(c -> ForumCategoryResDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 댓글 목록 가져오기 (최대 10개)
        Pageable commentPageable = Pageable.ofSize(10);
        var comments = commentQueryService.listByPost(p.getId(), commentPageable, viewerId).getContent();

        // 투표 정보 가져오기
        var vote = voteQueryService.getByPost(p.getId(), viewerId);

        return ForumPostResDto.builder()
                .id(p.getId())
                .stockId(p.getStockId())
                .createdBy(p.getCreatedBy())
                .title(p.getTitle())
                .contents(p.getContents())
                .imageUrl(p.getImageUrl())
                .status(p.getStatus())
                .likesCount(p.getLikesCount())
                .commentCount(p.getCommentCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .deletedAt(p.getDeletedAt())
                .version(p.getVersion())
                .categories(catDtos)
                .writerRole(p.getWriterRole())
                .likedByMe(likedByMe)
                .comments(comments)
                .vote(vote)
                .build();
    }

    private ForumPostResDto mapWithDetails(ForumPost p, boolean likedByMe, UUID viewerId) {
        List<ForumCategoryResDto> catDtos = p.getCategories().stream()
                .map(ForumPostCategory::getCategory)
                .map(c -> ForumCategoryResDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .build())
                .collect(Collectors.toList());

        // 댓글 목록 가져오기 (최대 10개)
        Pageable commentPageable = Pageable.ofSize(10);
        var comments = commentQueryService.listByPost(p.getId(), commentPageable, viewerId).getContent();

        // 투표 정보 가져오기
        var vote = voteQueryService.getByPost(p.getId(), viewerId);

        return ForumPostResDto.builder()
                .id(p.getId())
                .stockId(p.getStockId())
                .createdBy(p.getCreatedBy())
                .title(p.getTitle())
                .contents(p.getContents())
                .imageUrl(p.getImageUrl())
                .status(p.getStatus())
                .likesCount(p.getLikesCount())
                .commentCount(p.getCommentCount())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .deletedAt(p.getDeletedAt())
                .version(p.getVersion())
                .categories(catDtos)
                .writerRole(p.getWriterRole())
                .likedByMe(likedByMe)
                .comments(comments)
                .vote(vote)
                .build();
    }
}
