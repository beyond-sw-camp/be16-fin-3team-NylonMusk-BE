package com.beyond.MKX.common.auth.controller;

import com.beyond.MKX.common.auth.service.OCRService;
import com.beyond.MKX.common.apiResponse.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * OCR 컨트롤러
 * 
 * 신분증 이미지에서 이름, 생년월일 정보를 추출하는 API를 제공합니다.
 * 프론트엔드에서 자동 매칭을 위해 사용됩니다.
 */
@RestController
@RequestMapping("/api/public/ocr")
@RequiredArgsConstructor
public class OCRController {

    private final OCRService ocrService;

    /**
     * 신분증 OCR 처리 (이름, 생년월일 추출)
     * 
     * 처리 흐름:
     * 1. 업로드된 신분증 이미지로 OCR 처리 (NCP OCR API 호출)
     * 2. 이름, 생년월일 정보 추출 및 검증
     * 3. 구조화된 데이터로 반환
     * 
     * 응답 형식:
     * {
     *   "result": {
     *     "name": "홍길동",
     *     "birthDate": "1990-01-23",
     *     "registrationNumber": "900123-1234567"
     *   },
     *   "message": "신분증 정보 추출 성공"
     * }
     */
    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeIdCard(
            @RequestParam("file") MultipartFile file
    ) throws Exception {
        OCRService.IdCardInfoDto idCardInfo = ocrService.extractIdInfo(file);
        return ApiResponse.ok(idCardInfo.toMap(), "신분증 정보 추출 성공");
    }

    /**
     * 신분증 OCR 처리 (별칭: /id-card)
     * 
     * @see #analyzeIdCard(MultipartFile)
     */
    @PostMapping("/id-card")
    public ResponseEntity<?> uploadIdCard(
            @RequestParam("file") MultipartFile file) throws Exception {
        return analyzeIdCard(file);
    }
}