package com.beyond.MKX.domain.securities_firm.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.securities_firm.dto.SecuritiesFirmSummaryDto;
import com.beyond.MKX.domain.securities_firm.entity.SecuritiesFirm;
import com.beyond.MKX.domain.securities_firm.repository.SecuritiesFirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 승인된 증권사 공개 API
 * - 인증 불필요
 * - 일반 회원 가입 시 증권사 선택용
 */
@RestController
@RequestMapping("/public/securities-firms")
@RequiredArgsConstructor
public class SecuritiesFirmPublicController {

    private final SecuritiesFirmRepository securitiesFirmRepository;

    /**
     * 승인된 증권사 목록 조회 (페이징)
     *
     * @param search 검색어 (선택, 국문명 또는 영문명)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return Page<SecuritiesFirmSummaryDto>
     */
    @GetMapping("/active")
    public ResponseEntity<?> getActiveSecuritiesFirms(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "nameKo", direction = Sort.Direction.ASC)
            Pageable pageable
    ) {
        Page<SecuritiesFirm> page = securitiesFirmRepository.searchByStatusAndName(
                SecuritiesFirm.Status.ACTIVE,
                (search == null || search.trim().isEmpty()) ? null : search.trim(),
                pageable
        );

        // DTO로 변환
        Page<SecuritiesFirmSummaryDto> result = page.map(SecuritiesFirmSummaryDto::from);

        return ApiResponse.ok(result, "승인된 증권사 목록 조회 성공");
    }
}
