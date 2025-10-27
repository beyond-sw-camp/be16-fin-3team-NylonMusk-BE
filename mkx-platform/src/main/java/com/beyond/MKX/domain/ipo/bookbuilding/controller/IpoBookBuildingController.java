package com.beyond.MKX.domain.ipo.bookbuilding.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingAvailableResDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingCreateDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingIssuerViewDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingResDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.service.IpoBookBuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ipo/offerings")
@RequiredArgsConstructor
public class IpoBookBuildingController {
    private final IpoBookBuildingService bookBuildingService;
    private final AdminRepository adminRepository;

    /**
     * 🔹 수요예측 참여
     */
    @PostMapping("{offeringId}/book-building")
    public ResponseEntity<?> createBookBuilding(
            @AuthenticationPrincipal CustomAdminPrincipal principal,
            @PathVariable UUID offeringId,
            @RequestBody IpoBookBuildingCreateDTO dto) {
        IpoBookBuildingResDTO resDTO = bookBuildingService.create(offeringId, principal, dto);
        return ApiResponse.ok(resDTO, "수요예측 참여가 완료되었습니다.");
    }

    /**
     * 🔹 해당 공모의 수요예측 참여 내역
     */
    @GetMapping("{offeringId}/book-building")
    public ResponseEntity<?> getBookBuildingList(
            @AuthenticationPrincipal CustomAdminPrincipal principal,
            @PathVariable UUID offeringId) {

        Admin admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));
        if (admin.getCorporation() == null) {
            throw new IllegalArgumentException("기업 소속 관리자가 아닙니다.");
        }

        Corporation corporation = admin.getCorporation();
        if (corporation == null) {
            throw new IllegalArgumentException("기업 소속 관리자가 아닙니다.");
        }

        UUID participantId = corporation.getId();

        List<IpoBookBuildingResDTO> resDTOList = bookBuildingService.findAllByOfferingId(offeringId, participantId);
        return ApiResponse.ok(resDTOList, "해당 기업의 수요예측 내역 조회 완료");
    }

    /**
     * 🔹 수요예측 결과 기반 확정공모가 산정
     */
    @PatchMapping("{offeringId}/book-building/finalize")
    public ResponseEntity<?> fixOfferPrice(@PathVariable UUID offeringId) {
        var fixOfferPrice = bookBuildingService.fixOfferPriceByBookBuilding(offeringId);
        return ApiResponse.ok(fixOfferPrice, "수요예측 결과를 기반으로 확정공모가가 산정되었습니다.");
    }

    /**
     * 🔹 수요예측 참여 가능한 공모 목록 조회
     */
    @GetMapping("/book-building/available")
    public ResponseEntity<?> bookBuildingAvailable(@AuthenticationPrincipal CustomAdminPrincipal principal) {
        Admin admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));
        if (admin.getCorporation() == null) {
            throw new IllegalArgumentException("기업 소속 관리자가 아닙니다.");
        }
        UUID participantId = admin.getCorporation().getId();

        List<IpoBookBuildingAvailableResDTO> result =
                bookBuildingService.findAllScheduledOfferings(participantId);

        return ApiResponse.ok(result, "수요예측 가능한 공모 리스트입니다.");
    }

    @GetMapping("{offeringId}/book-building/issuer-view")
    public ResponseEntity<?> getAllBookBuildingForIssuer(
            @AuthenticationPrincipal CustomAdminPrincipal principal,
            @PathVariable UUID offeringId) {

        Admin admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        UUID issuerCorpId = admin.getCorporation().getId();
        List<IpoBookBuildingIssuerViewDTO> list = bookBuildingService.findAllForIssuer(offeringId, issuerCorpId);
        return ApiResponse.ok(list, "발행사용 수요예측 참여 현황 조회 완료");
    }
}
