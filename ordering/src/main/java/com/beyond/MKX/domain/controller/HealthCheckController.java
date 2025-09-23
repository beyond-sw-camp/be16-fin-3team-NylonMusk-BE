package com.beyond.MKX.domain.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        String response = "ordering-service: OK";
        return ApiResponse.ok(response);
    }
}
