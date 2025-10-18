package com.beyond.MKX.domain.disclosure.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CorporationOnly;
import com.beyond.MKX.domain.disclosure.dto.DisclosureRegisterReqFormDto;
import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.service.DisclosureService;
import com.beyond.MKX.domain.disclosure.dto.DisclosureUpdateFileReqDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/disclosures")
public class DisclosureController {

    private final DisclosureService disclosureService;

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
}
