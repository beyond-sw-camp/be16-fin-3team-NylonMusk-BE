package com.beyond.MKX.domain.ipo.bookbuilding.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingCreateDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingResDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.service.IpoBookBuildingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/ipo/offerings")
@RequiredArgsConstructor
public class IpoBookBuildingController {
    private final IpoBookBuildingService bookBuildingService;

    @PostMapping("{offeringId}/book-building")
    public ResponseEntity<?> createBookBuilding(@PathVariable UUID offeringId, @RequestBody IpoBookBuildingCreateDTO dto) {
        IpoBookBuildingResDTO resDTO = bookBuildingService.create(dto);
        return ApiResponse.ok(resDTO, "수요예측 참여가 완료되었습니다.");
    }

    @GetMapping("{offeringId}/book-building")
    public ResponseEntity<?> getBookBuildingList(@PathVariable UUID offeringId) {
        List<IpoBookBuildingResDTO> resDTOList = bookBuildingService.findAllByOfferingId(offeringId);
        return ApiResponse.ok(resDTOList, "수요예측 내역 조회 완료");
    }
}
