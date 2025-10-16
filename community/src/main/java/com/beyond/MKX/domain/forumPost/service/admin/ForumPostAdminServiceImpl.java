package com.beyond.MKX.domain.forumPost.service.admin;

import com.beyond.MKX.domain.forumCategory.dto.ForumCategoryResDto;
import com.beyond.MKX.domain.forumPost.dto.ForumPostResDto;
import com.beyond.MKX.domain.forumPost.entity.ForumPost;
import com.beyond.MKX.domain.forumPost.entity.ForumPostCategory;
import com.beyond.MKX.domain.forumPost.repository.ForumPostAdminRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForumPostAdminServiceImpl implements ForumPostAdminService {

    private final ForumPostAdminRepository adminRepo;

    @Override
    public Page<ForumPostResDto> listAllIncludingDeleted(Pageable pageable) {
        return adminRepo.findAllIncludingDeleted(pageable).map(this::map);
    }

    @Override
    public Page<ForumPostResDto> listDeleted(Pageable pageable) {
        return adminRepo.findAllDeleted(pageable).map(this::map);
    }

    @Override
    public ForumPostResDto getIncludingDeleted(UUID id) {
        ForumPost p = adminRepo.findByIdIncludingDeleted(id)
                .orElseThrow(() -> new EntityNotFoundException("ForumPost not found: " + id));
        return map(p);
    }

    @Transactional
    @Override
    public void restore(UUID id) {
        int updated = adminRepo.restoreById(id);
        if (updated == 0) throw new EntityNotFoundException("ForumPost not found: " + id);
    }

    @Transactional
    @Override
    public void hardDelete(UUID id) {
        // FK 보호 위해 조인 테이블 먼저
        adminRepo.deleteCategoriesByPostId(id);
        int deleted = adminRepo.hardDeleteById(id);
        if (deleted == 0) throw new EntityNotFoundException("ForumPost not found: " + id);
    }

    private ForumPostResDto map(ForumPost p) {
        List<ForumCategoryResDto> cats = p.getCategories().stream()
                .map(ForumPostCategory::getCategory)
                .map(c -> ForumCategoryResDto.builder()
                        .id(c.getId())
                        .name(c.getName())
                        .description(c.getDescription())
                        .build())
                .toList();

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
                .categories(cats)
                .writerRole(p.getWriterRole())
                .build();
    }
}
