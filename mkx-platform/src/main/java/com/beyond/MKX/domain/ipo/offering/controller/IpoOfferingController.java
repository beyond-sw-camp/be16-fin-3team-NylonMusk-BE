package com.beyond.MKX.domain.ipo.offering.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ipo.offering.dto.IpoOfferingPriceFixReqDTO;
import com.beyond.MKX.domain.ipo.offering.dto.IpoOfferingReqDTO;
import com.beyond.MKX.domain.ipo.offering.dto.IpoOfferingResDTO;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.service.IpoOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/ipo")
@RequiredArgsConstructor
@Validated
public class IpoOfferingController {
    private final IpoOfferingService offeringService;

    /* 공모 생성 */
    @PostMapping("/{ipoId}/offerings")
    public ResponseEntity<?> create(@PathVariable UUID ipoId, @Valid @RequestBody IpoOfferingReqDTO dto) {
        IpoOffering saved = offeringService.create(ipoId, dto);
        return ApiResponse.ok(IpoOfferingResDTO.from(saved), "공모 차수 등록이 완료되었습니다.");
    }

    /* 공모 조회 */
    @GetMapping("/offerings/{offeringId}")
    public ResponseEntity<?> findById(@PathVariable UUID offeringId) {
        IpoOfferingResDTO info = offeringService.findById(offeringId);
        return ApiResponse.ok(info, "공모 조회 결과입니다.");
    }

    /* 공모가 확정 */
    @PatchMapping("/offerings/{offeringId}/fixed-price")
    public ResponseEntity<?> fixOfferPrice(@PathVariable UUID offeringId, @Valid @RequestBody IpoOfferingPriceFixReqDTO fixReqDTO) {
        IpoOffering fixPrice = offeringService.fixOfferPrice(offeringId, fixReqDTO.getOfferPrice());
        return ApiResponse.ok(IpoOfferingResDTO.from(fixPrice), "확정 공모가가 등록되었습니다.");
    }

    /* 공모 청약 오픈 */
    @PatchMapping("/offerings/{offeringId}/open")
    public ResponseEntity<?> open(@PathVariable UUID offeringId) {
        IpoOffering opened = offeringService.open(offeringId); // SCHEDULED -> OPEN (청약 오픈)
        return ApiResponse.ok(IpoOfferingResDTO.from(opened), "청약이 개시되었습니다.");
    }

    /* 공모 청약 마감 */
    @PatchMapping("/offerings/{offeringId}/close")
    public ResponseEntity<?> close(@PathVariable UUID offeringId) {
        IpoOffering closed = offeringService.close(offeringId); // OPEN -> CLOSED (집계 준비)
        return ApiResponse.ok(IpoOfferingResDTO.from(closed), "청약이 마감되었습니다.");
    }

    /* 공모 청약 취소 */
    @PatchMapping("/offerings/{offeringId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID offeringId) {
        IpoOffering cancelled = offeringService.cancel(offeringId); // SCHEDULED/OPEN 한정 -> CANCELLED
        return ApiResponse.ok(IpoOfferingResDTO.from(cancelled), "공모가 취소되었습니다.");
    }

    @GetMapping("/{ipoId}/offerings/list-status")
    public ResponseEntity<?> listByIpo(@PathVariable UUID ipoId,
                                       @RequestParam(required = false, name = "statuses")
                                       List<IpoOfferingStatus> statuses,
                                       Pageable pageable) {
        var page = offeringService.listByIpo(ipoId, statuses, pageable);
        return ApiResponse.ok(page, "공모 목록입니다.");
    }

    @GetMapping("/offerings/list")
    public ResponseEntity<?> listAll(@RequestParam(required = false, name = "statuses")
                                     List<IpoOfferingStatus> statuses,
                                     @RequestParam(defaultValue = "false") boolean onlySubscribable,
                                     Pageable pageable) {
        var page = offeringService.listAll(statuses, onlySubscribable, pageable);
        return ApiResponse.ok(page, "공모 목록입니다.");
    }

    @PatchMapping("/offerings/{offeringId}/book-building/start")
    public ResponseEntity<?> changeStatus(@PathVariable UUID offeringId) {
        IpoOffering offering = offeringService.startBookBuilding(offeringId);
        return ApiResponse.ok(IpoOfferingResDTO.from(offering), "SCHEDULED ~> BOOK_BUILDING 상태변경 완료");
    }
}
