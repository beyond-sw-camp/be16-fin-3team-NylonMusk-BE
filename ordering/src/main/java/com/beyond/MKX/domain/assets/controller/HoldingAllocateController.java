package com.beyond.MKX.domain.assets.controller;

import com.beyond.MKX.domain.assets.controller.AllocateHoldingsBatchReq;
import com.beyond.MKX.domain.assets.service.HoldingAllocateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/holdings")
@RequiredArgsConstructor
public class HoldingAllocateController {

    private final HoldingAllocateService service;

    @PostMapping("/allocate/batch")
    public ResponseEntity<Void> allocateBatch(@Valid @RequestBody AllocateHoldingsBatchReq req) {
        service.allocateBatch(req);
        return ResponseEntity.ok().build();
    }
}
