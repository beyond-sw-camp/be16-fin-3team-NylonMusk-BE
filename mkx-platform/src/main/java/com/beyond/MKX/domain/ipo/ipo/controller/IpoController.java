package com.beyond.MKX.domain.ipo.ipo.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ipo.ipo.dto.*;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.ipo.service.IpoApprovalService;
import com.beyond.MKX.domain.ipo.ipo.service.IpoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/ipo")
public class IpoController {

    private final IpoService ipoService;
    private final IpoApprovalService ipoApprovalService;

    @PostMapping("/request")
    public ResponseEntity<?> createReq(@ModelAttribute IpoCreateReqDTO ipoCreateReqDTO) {
        IpoCreateResDTO ipoCreateResDTO = ipoService.createRequest(ipoCreateReqDTO);
        return ApiResponse.ok(ipoCreateResDTO, "IPO 상장 요청이 성공적으로 접수되었습니다.");
    }

    @GetMapping("/admin/requests")
    // @PreAuthorize("hasRole('EXCHANGE')") 권장
    public ResponseEntity<?> list(
            @RequestParam(required = false) List<IpoStatus> status,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<IpoRequestItemDTO> page = ipoApprovalService.listRequests(
                (status == null ? null : Set.copyOf(status)), q, from, to, pageable);
        return ApiResponse.ok(page, "상장 요청 목록입니다.");
    }

    @PostMapping("/{ipoId}/review")
    public ResponseEntity<?> adminReview(@PathVariable UUID ipoId, @RequestBody @Valid IpoReviewReqDTO
            ipoReviewReqDTO) {
        Ipo ipo = ipoService.adminReview(ipoId, ipoReviewReqDTO);
        String message = Boolean.TRUE.equals(ipoReviewReqDTO.getApprove())
                ? "상장 요청이 승인되었습니다. 심사 완료 상태로 전환합니다."
                : "상장 요청이 반려되었습니다. 반려 사유 : " + ipoReviewReqDTO.getRejectReason();
        return ApiResponse.ok(ipo.getId(), message);
    }

    @PostMapping("/{ipoId}/list")
    public ResponseEntity<?> adminListing(@PathVariable UUID ipoId, @RequestBody(required = false) IpoListReqDTO priceOnListing) {
        Long requestedPrice = (priceOnListing == null) ? null : priceOnListing.getPriceOnListing();
        IpoListResDTO dto = ipoService.list(ipoId, requestedPrice);
        return ApiResponse.ok(dto, "기업의 상장 요청이 정상적으로 저장되었습니다.");
    }


}
