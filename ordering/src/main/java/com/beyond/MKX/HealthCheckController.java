package com.beyond.MKX;

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

    @GetMapping("/health2")
    public ResponseEntity<?> healthCheck2() {
        String response = "ordering-service: OK";
        return ApiResponse.ok(response);
    }
}
