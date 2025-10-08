package com.beyond.MKX.domain.ipo.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.domain.ipo.dto.IpoOfferingReqDTO;
import com.beyond.MKX.domain.ipo.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.service.IpoOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ipo/offering")
@RequiredArgsConstructor
@Validated
public class IpoOfferingController {
    private final IpoOfferingService offeringService;

    /* 공모 생성 */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody IpoOfferingReqDTO dto) {
        IpoOffering saved = offeringService.create(dto);
        return ApiResponse.ok("공모 차수 등록이 완료되었습니다.", saved.toString());
    }
}
