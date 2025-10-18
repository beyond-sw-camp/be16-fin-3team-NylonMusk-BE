package com.beyond.MKX.domain.disclosure.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CorporationOnly;
import com.beyond.MKX.domain.disclosure.dto.DisclosureRegisterReqFormDto;
import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.service.DisclosureService;
import com.beyond.MKX.domain.disclosure.dto.DisclosureUpdateFileReqDto;
import com.beyond.MKX.domain.disclosure.service.DisclosureCorpQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;

@RestController
@RequiredArgsConstructor
@RequestMapping("/disclosures")
public class DisclosureController {

    private final DisclosureService disclosureService;
    private final DisclosureCorpQueryService disclosureCorpQueryService;

    /**
     * 기업용 공시 등록 (Multipart)
     */
    @CorporationOnly
    @PostMapping(value = "/corp/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerMultipart(@ModelAttribute DisclosureRegisterReqFormDto request) {
        Disclosure saved = disclosureService.register(request);
        return ApiResponse.created(DisclosureMapper.toRes(saved), "공시 등록 완료");
    }

    /**
     * 공시 파일 수정 (Multipart)
     */
    @CorporationOnly
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateFile(
            @PathVariable UUID id,
            @ModelAttribute DisclosureUpdateFileReqDto request
    ) {
        Disclosure updated = disclosureService.updateFile(id, request.getFile(), request.getSummary());
        return ApiResponse.ok(DisclosureMapper.toRes(updated), "공시 파일 수정 완료");
    }

    /**
     * 내 기업 공시 목록 조회(상태/유형/제목/기간 필터)
     */
    @CorporationOnly
    @GetMapping("/my")
    public ResponseEntity<?> listMine(
            @RequestParam(required = false) DisclosureStatus status,
            @RequestParam(required = false) DisclosureType type,
            @RequestParam(required = false, name = "title") String title,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate fromDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @RequestParam(required = false) LocalDate toDate,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<DisclosureResDto> page = disclosureCorpQueryService.listMine(status, type, title, fromDate, toDate, pageable);
        return ApiResponse.ok(page, "기업 공시 조회 완료");
    }
}
