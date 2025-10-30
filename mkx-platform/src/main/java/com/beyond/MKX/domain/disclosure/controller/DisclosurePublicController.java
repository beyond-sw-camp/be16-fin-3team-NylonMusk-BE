package com.beyond.MKX.domain.disclosure.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.service.DisclosureAdminQueryService;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.service.DisclosureQueryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/disclosures")
public class DisclosurePublicController {

    private final DisclosureQueryService disclosureQueryService;
    private final DisclosureAdminQueryService disclosureAdminQueryService;

    /**
     * 승인된 공시 목록 조회(유형/종목/검색어 필터)
     */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(required = false) DisclosureType type,
            @RequestParam(required = false) String ticker,
            @RequestParam(required = false, name = "title") String title,
            @RequestParam(required = false, name = "displayNo") String displayNo,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<DisclosureResDto> page = disclosureQueryService.listApproved(type, ticker, title, displayNo, pageable);
        return ApiResponse.ok(page, "승인 공시 조회 완료");
    }

    /** 공개용 목록 배치: 다중 티커 */
    @GetMapping("/batch")
    public ResponseEntity<?> listBatch(
            @RequestParam List<String> tickers,
            @RequestParam(required = false) DisclosureType type,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<DisclosureResDto> page = disclosureQueryService.listApprovedByTickers(tickers, type, pageable);
        return ApiResponse.ok(page, "승인 공시 배치 조회 완료");
    }

    /** 공개용 상세: 특정 공시번호의 최신 승인본 */
    @GetMapping("/{displayNo}")
    public ResponseEntity<?> latestByDisplayNo(@PathVariable String displayNo) {
        List<DisclosureResDto> chain = disclosureAdminQueryService.listRevisionsByDisplayNo(displayNo)
                .stream()
                .filter(d -> d.status() == DisclosureStatus.APPROVED)
                .toList();
        if (chain.isEmpty()) {
            throw new EntityNotFoundException("해당 공시번호를 찾을 수 없습니다.");
        }
        return ApiResponse.ok(chain.get(0), "승인 공시 상세 조회 완료");
    }

    /** 공개용 히스토리: 특정 공시번호의 정정 이력(최신 우선) */
    @GetMapping("/{displayNo}/history")
    public ResponseEntity<?> historyByDisplayNo(@PathVariable String displayNo) {
        List<DisclosureResDto> history = disclosureAdminQueryService.listRevisionsByDisplayNo(displayNo)
                .stream()
                .filter(d -> d.status() == DisclosureStatus.APPROVED)
                .toList();
        if (history.isEmpty()) {
            throw new EntityNotFoundException("해당 공시번호를 찾을 수 없습니다.");
        }
        return ApiResponse.ok(history, "승인 공시 이력 조회 완료");
    }
}
