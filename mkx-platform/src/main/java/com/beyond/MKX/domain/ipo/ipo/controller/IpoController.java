package com.beyond.MKX.domain.ipo.ipo.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoCreateReqDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoCreateResDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoListReqDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoReviewReqDTO;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.service.IpoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ipo")
public class IpoController {

    private final IpoService ipoService;

    @PostMapping("/request")
    public ResponseEntity<?> createReq(@ModelAttribute IpoCreateReqDTO ipoCreateReqDTO) {
        IpoCreateResDTO ipoCreateResDTO = ipoService.createRequest(ipoCreateReqDTO);
        return ApiResponse.ok(ipoCreateResDTO, "IPO 상장 요청이 성공적으로 접수되었습니다.");
    }

        @PostMapping("/{ipoId}/review")
        public ResponseEntity<?> adminReview (@PathVariable UUID ipoId, @RequestBody @Valid IpoReviewReqDTO
        ipoReviewReqDTO){
            Ipo ipo = ipoService.adminReview(ipoId, ipoReviewReqDTO);
            String message = Boolean.TRUE.equals(ipoReviewReqDTO.getApprove())
                    ? "상장 요청이 승인되었습니다. 심사 완료 상태로 전환합니다."
                    : "상장 요청이 반려되었습니다. 반려 사유 : " + ipoReviewReqDTO.getRejectReason();
            return ApiResponse.ok(ipo.getId(), message);
        }

        @PostMapping("/{ipoId}/list")
        public ResponseEntity<?> adminListing (@PathVariable UUID ipoId, @RequestBody @Valid IpoListReqDTO ipoListReqDTO)
        {
            Ipo ipo = ipoService.list(ipoId, ipoListReqDTO);
            return ApiResponse.ok(ipo.getListingAt(), "기업의 상장 요청이 정상적으로 저장되었습니다.");
        }


    }
