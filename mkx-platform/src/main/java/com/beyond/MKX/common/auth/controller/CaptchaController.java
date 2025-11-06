package com.beyond.MKX.common.auth.controller;

import com.beyond.MKX.common.apiResponse.ApiResponse;
import com.beyond.MKX.common.auth.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * CAPTCHA_CONTROLLER: CAPTCHA 키 생성 API
 * - CAPTCHA 키 생성 (이미지/음성 URL 포함)
 */
@Slf4j
@RestController
@RequestMapping("/api/public/captcha")
@RequiredArgsConstructor
public class CaptchaController {

    private final CaptchaService captchaService;
    private final RestTemplate restTemplate;
    
    @Value("${ncp.captcha.client-id}")
    private String clientId;
    
    @Value("${ncp.captcha.secret-key}")
    private String secretKey;

    /**
     * GENERATE_CAPTCHA_KEY: CAPTCHA 키 생성
     * GET /api/public/captcha/key
     * 응답: { key: "xxx", imageUrl: "...", audioUrl: "..." }
     */
    @GetMapping("/key")
    public ResponseEntity<?> generateCaptchaKey() {
        try {
            String key = captchaService.generateCaptchaKey();
            if (key == null || key.isEmpty()) {
                log.error("CAPTCHA 키 생성 실패: key가 null이거나 비어있음");
                return ApiResponse.error(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "CAPTCHA 키 생성에 실패했습니다. 잠시 후 다시 시도해주세요.",
                    null
                );
            }
            
            // 음성 CAPTCHA 키도 생성 (별도 키)
            String audioKey = captchaService.generateAudioCaptchaKey();
            
            // 프록시 URL로 변경 (백엔드를 통해 이미지/음성 제공)
            String proxyImageUrl = "/mkx-platform-service/api/public/captcha/image?key=" + key;
            String proxyAudioUrl = audioKey != null 
                ? "/mkx-platform-service/api/public/captcha/audio?key=" + audioKey
                : "/mkx-platform-service/api/public/captcha/audio?key=" + key; // 음성 키 생성 실패 시 이미지 키 사용
            
            Map<String, String> result = new HashMap<>();
            result.put("key", key); // 이미지 키
            if (audioKey != null) {
                result.put("audioKey", audioKey); // 음성 키
            }
            result.put("imageUrl", proxyImageUrl);
            result.put("audioUrl", proxyAudioUrl);
            
            log.info("CAPTCHA 키 생성 완료: key={}", key);
            return ApiResponse.ok(result, "CAPTCHA 키가 생성되었습니다.");
        } catch (Exception e) {
            log.error("CAPTCHA 키 생성 중 예외 발생", e);
            return ApiResponse.error(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                "CAPTCHA 키 생성 중 오류가 발생했습니다: " + e.getMessage(),
                null
            );
        }
    }

    /**
     * GET_CAPTCHA_IMAGE: CAPTCHA 이미지 프록시 (인증 헤더 포함)
     * GET /api/public/captcha/image?key={key}
     */
    @GetMapping("/image")
    public ResponseEntity<Resource> getCaptchaImage(@RequestParam String key) {
        try {
            String imageUrl = captchaService.getCaptchaImageUrl(key);
            log.info("CAPTCHA 이미지 요청: url={}, key={}", imageUrl, key);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", secretKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imageUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );
            
            log.info("CAPTCHA 이미지 API 응답: status={}, contentType={}, bodySize={}", 
                response.getStatusCode(), 
                response.getHeaders().getContentType(),
                response.getBody() != null ? response.getBody().length : 0);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ByteArrayResource resource = new ByteArrayResource(response.getBody());
                
                // NCP API 응답의 Content-Type 확인
                MediaType contentType = response.getHeaders().getContentType();
                if (contentType == null) {
                    contentType = MediaType.IMAGE_PNG; // 기본값
                }
                
                return ResponseEntity.ok()
                        .contentType(contentType)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .header(HttpHeaders.PRAGMA, "no-cache")
                        .body(resource);
            }
            
            log.warn("CAPTCHA 이미지 API 응답 실패: status={}", response.getStatusCode());
            return ResponseEntity.notFound().build();
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("CAPTCHA 이미지 HTTP 클라이언트 에러: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("CAPTCHA 이미지 로드 실패: key={}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET_CAPTCHA_AUDIO: CAPTCHA 음성 프록시
     * GET /api/public/captcha/audio?key={key}
     * 음성 CAPTCHA는 헤더 없이 URL 파라미터로만 인증
     */
    @GetMapping("/audio")
    public ResponseEntity<Resource> getCaptchaAudio(@RequestParam String key) {
        try {
            String audioUrl = captchaService.getCaptchaAudioUrl(key);
            
            // 음성 CAPTCHA는 헤더 없이 URL 파라미터로만 인증
            HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());
            
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    audioUrl,
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                ByteArrayResource resource = new ByteArrayResource(response.getBody());
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=captcha.wav")
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .body(resource);
            }
            
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("CAPTCHA 음성 로드 실패: key={}", key, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

