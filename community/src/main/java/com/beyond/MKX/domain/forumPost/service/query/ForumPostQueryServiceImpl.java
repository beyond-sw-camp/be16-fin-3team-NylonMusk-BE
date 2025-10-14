package com.beyond.MKX.domain.forumPost.service.query;

import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.entity.PostStatus;
import com.beyond.MKX.domain.forumPost.repository.ForumPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumPostQueryServiceImpl implements ForumPostQueryService {

    private final ForumPostRepository repo;

    @Override
    public Page<ForumPostResDto> list(PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? repo.findAllBy(pageable)
                : repo.findByStatus(status, pageable);
        return page.map(this::map);
    }

    @Override
    public Page<ForumPostResDto> listMine(UUID me, PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? repo.findByCreatedBy(me, pageable)
                : repo.findByCreatedByAndStatus(me, status, pageable);
        return page.map(this::map);
    }

    @Override
    public Page<ForumPostResDto> listByUser(UUID userId, PostStatus status, Pageable pageable) {
        Page<ForumPost> page = (status == null)
                ? repo.findByCreatedBy(userId, pageable)
                : repo.findByCreatedByAndStatus(userId, status, pageable);
        return page.map(this::map);
    }

    private ForumPostResDto map(ForumPost p) {
        return new ForumPostResDto(
                p.getId(), p.getStockId(), p.getCreatedBy(), p.getTitle(), p.getContents(),
                p.getImageUrl(), p.getStatus(), p.getLikesCount(), p.getCommentCount(),
                p.getCreatedAt(), p.getUpdatedAt(), p.getDeletedAt(), p.getVersion()
        );
    }
}
