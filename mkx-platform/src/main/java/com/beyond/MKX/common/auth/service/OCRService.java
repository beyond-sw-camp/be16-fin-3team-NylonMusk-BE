package com.beyond.MKX.common.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class OCRService {

    @Value("${ncp.ocr.url}")
    private String ocrUrl;

    @Value("${ncp.ocr.secret-key}")
    private String secretKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 주민등록번호 형식 검증 (13자리 숫자: YYMMDD-GXXXXXX)
    private static final Pattern REGISTRATION_NUMBER_PATTERN = Pattern.compile("^\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])-?[1-4]\\d{6}$");
    
    // 파일 크기 제한: 1 ~ 52428800 bytes (약 50MB)
    private static final long MAX_FILE_SIZE = 52_428_800L;
    private static final long MIN_FILE_SIZE = 1L;
    
    // 허용된 이미지 형식
    private static final String[] ALLOWED_IMAGE_TYPES = {"jpg", "jpeg", "png"};

    /**
     * 신분증 OCR 처리 및 구조화된 정보 추출
     */
    public IdCardInfoDto extractIdInfo(MultipartFile file) throws IOException {
        // FILE_VALIDATION: 파일 검증
        validateFile(file);
        
        // 파일 바이트를 먼저 읽어서 메모리에 저장 (Tomcat 임시 파일 삭제 대비)
        byte[] fileBytes = file.getBytes();

        // MESSAGE_JSON: NCP OCR API V2 요청 메시지 구성
        // multipart/form-data 방식에서는 file 필드에 실제 파일을 보내고,
        // message 필드에는 format, name 등 메타데이터만 포함합니다.
        Map<String, Object> message = new HashMap<>();
        message.put("version", "V2");
        message.put("requestId", "req-" + System.currentTimeMillis());
        message.put("timestamp", System.currentTimeMillis());

        // IMAGE_FORMAT: 파일 확장자에서 이미지 형식 추출
        String imageFormat = extractImageFormat(file.getOriginalFilename());
        
        Map<String, Object> image = new HashMap<>();
        image.put("format", imageFormat);
        image.put("name", file.getOriginalFilename() != null ? file.getOriginalFilename() : "idcard");
        // 주의: multipart/form-data 방식에서는 data 필드를 포함하지 않습니다.
        // 실제 이미지 데이터는 file 필드로 별도 전송합니다.
        message.put("images", new Object[]{image});

        String messageJson = objectMapper.writeValueAsString(message);

        // ✅ 2️⃣ 헤더 구성
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.add("X-OCR-SECRET", secretKey);

        // ✅ 3️⃣ 멀티파트 구성
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("message", messageJson);
        body.add("file", new org.springframework.core.io.ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        // OCR_API_CALL: 요청 전송 및 에러 처리
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(ocrUrl, requestEntity, String.class);
            
            // OCR_RESPONSE_PARSE: 응답 파싱 및 구조화
            JsonNode root = objectMapper.readTree(response.getBody());
            return parseIdCardResponse(root);
            
        } catch (HttpClientErrorException e) {
            // NCP OCR API 에러 응답 파싱
            String errorMessage = parseOcrApiError(e.getResponseBodyAsString());
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * OCR 응답을 구조화된 DTO로 파싱
     */
    private IdCardInfoDto parseIdCardResponse(JsonNode root) {
        JsonNode images = root.path("images");
        if (!images.isArray() || images.size() == 0) {
            throw new IllegalArgumentException("OCR 응답에 이미지 정보가 없습니다.");
        }

        JsonNode imageNode = images.get(0);
        String inferResult = imageNode.path("inferResult").asText();
        String message = imageNode.path("message").asText();

        if (!"SUCCESS".equalsIgnoreCase(inferResult)) {
            throw new IllegalArgumentException("OCR 처리 실패: " + message);
        }

        JsonNode idCardNode = imageNode.path("idCard");
        if (idCardNode.isMissingNode() || idCardNode.isNull()) {
            throw new IllegalArgumentException("신분증 정보를 찾을 수 없습니다.");
        }

        // NCP OCR 응답 구조: idCard.result.ic.name[0].formatted.value 또는 text
        JsonNode resultNode = idCardNode.path("result");
        if (resultNode.isMissingNode() || resultNode.isNull()) {
            throw new IllegalArgumentException("신분증 결과 정보를 찾을 수 없습니다.");
        }

        JsonNode icNode = resultNode.path("ic");
        if (icNode.isMissingNode() || icNode.isNull()) {
            throw new IllegalArgumentException("신분증 IC 정보를 찾을 수 없습니다.");
        }

        // NAME_EXTRACT: 이름 추출 (idCard.result.ic.name[0].formatted.value 또는 text)
        String name = extractNameFromIc(icNode);
        
        // BIRTHDATE_EXTRACT: 생년월일 추출 (주민등록번호에서 추출하지만 DTO에는 저장하지 않음)
        String birthDate = extractBirthDateFromIc(icNode);

        // VALIDATION: 필수 필드 검증
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 추출할 수 없습니다.");
        }

        if (birthDate == null || birthDate.isBlank()) {
            throw new IllegalArgumentException("생년월일을 추출할 수 없습니다.");
        }

        return IdCardInfoDto.builder()
                .name(name)
                .birthDate(birthDate)
                .rawData(idCardNode)
                .build();
    }

    /**
     * 이름 추출 (idCard.result.ic.name[0].formatted.value 또는 text)
     */
    private String extractNameFromIc(JsonNode icNode) {
        JsonNode nameArray = icNode.path("name");
        if (!nameArray.isArray() || nameArray.size() == 0) {
            return null;
        }

        JsonNode nameObj = nameArray.get(0);
        if (nameObj == null || nameObj.isNull()) {
            return null;
        }

        // formatted.value 우선 시도
        JsonNode formattedNode = nameObj.path("formatted");
        if (!formattedNode.isMissingNode() && !formattedNode.isNull()) {
            JsonNode valueNode = formattedNode.path("value");
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                String value = valueNode.asText();
                if (value != null && !value.isBlank()) {
                    // 마스킹 제거 (예: "홍**" -> "홍")
                    String cleanedValue = value.replaceAll("\\*+", "").trim();
                    if (!cleanedValue.isEmpty()) {
                        return cleanedValue;
                    } else {
                        // 마스킹만 있는 경우, 원본 값을 그대로 반환
                        return value.trim();
                    }
                }
            }
        }

        // text 필드 시도
        JsonNode textNode = nameObj.path("text");
        if (!textNode.isMissingNode() && !textNode.isNull()) {
            String value = textNode.asText();
            if (value != null && !value.isBlank()) {
                // 마스킹 제거
                String cleanedValue = value.replaceAll("\\*+", "").trim();
                if (!cleanedValue.isEmpty()) {
                    return cleanedValue;
                } else {
                    // 마스킹만 있는 경우, 원본 값을 그대로 반환
                    return value.trim();
                }
            }
        }

        return null;
    }

    /**
     * 필드 추출 및 정제 (공백, 특수문자 처리)
     */
    private String extractAndCleanField(JsonNode idCardNode, String fieldName) {
        JsonNode fieldNode = idCardNode.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }

        String value = fieldNode.asText();
        if (value == null || value.isBlank()) {
            return null;
        }

        // 공백 정제 (앞뒤 공백 제거, 중복 공백 제거)
        value = value.trim().replaceAll("\\s+", " ");
        return value.isEmpty() ? null : value;
    }

    /**
     * 주민등록번호 추출 및 정제
     */
    private String extractAndCleanRegistrationNumber(JsonNode idCardNode, String fieldName) {
        JsonNode fieldNode = idCardNode.path(fieldName);
        if (fieldNode.isMissingNode() || fieldNode.isNull()) {
            return null;
        }

        String value = fieldNode.asText();
        if (value == null || value.isBlank()) {
            return null;
        }

        // 숫자와 하이픈만 남기기
        value = value.replaceAll("[^0-9-]", "");
        
        // 하이픈이 없으면 추가 (YYMMDD-GXXXXXX 형식)
        if (value.length() == 13 && !value.contains("-")) {
            value = value.substring(0, 6) + "-" + value.substring(6);
        }

        return value;
    }

    /**
     * 생년월일 추출 (주민등록번호에서 직접 추출)
     */
    private String extractBirthDateFromIc(JsonNode icNode) {
        JsonNode personalNumArray = icNode.path("personalNum");
        if (!personalNumArray.isArray() || personalNumArray.size() == 0) {
            return null;
        }

        JsonNode personalNumObj = personalNumArray.get(0);
        if (personalNumObj == null || personalNumObj.isNull()) {
            return null;
        }

        String personalNumText = null;

        // formatted.value 우선 시도
        JsonNode formattedNode = personalNumObj.path("formatted");
        if (!formattedNode.isMissingNode() && !formattedNode.isNull()) {
            JsonNode valueNode = formattedNode.path("value");
            if (!valueNode.isMissingNode() && !valueNode.isNull()) {
                personalNumText = valueNode.asText();
            }
        }

        // text 필드 시도 (formatted.value가 없을 경우)
        if (personalNumText == null || personalNumText.isBlank()) {
            JsonNode textNode = personalNumObj.path("text");
            if (!textNode.isMissingNode() && !textNode.isNull()) {
                personalNumText = textNode.asText();
            }
        }

        if (personalNumText == null || personalNumText.isBlank()) {
            return null;
        }

        // 숫자만 추출 (마스킹 문자 제거)
        String numbers = personalNumText.replaceAll("[^0-9]", "");
        
        if (numbers.length() < 6) {
            return null;
        }

        // 앞 6자리 추출 (YYMMDD)
        String yymmdd = numbers.substring(0, 6);
        
        if (!yymmdd.matches("\\d{6}")) {
            return null;
        }
        
        String yy = yymmdd.substring(0, 2);
        String mm = yymmdd.substring(2, 4);
        String dd = yymmdd.substring(4, 6);

        // 세기 판단 (주민등록번호 7번째 자리: 1,2=1900년대, 3,4=2000년대)
        int century = 1900;
        if (numbers.length() >= 7) {
            char genderChar = numbers.charAt(6);
            if (Character.isDigit(genderChar)) {
                int genderCode = Character.getNumericValue(genderChar);
                if (genderCode == 3 || genderCode == 4) {
                    century = 2000;
                }
            }
        }

        int year = Integer.parseInt(yy) + century;
        
        // 월/일 유효성 검사
        int month = Integer.parseInt(mm);
        int day = Integer.parseInt(dd);
        if (month < 1 || month > 12) {
            return null;
        }
        if (day < 1 || day > 31) {
            return null;
        }
        
        return String.format("%d-%s-%s", year, mm, dd);
    }

    /**
     * 파일 검증 (크기, 타입, Content-Type)
     * 
     * NCP OCR API 지원 형식:
     * - 이미지: jpg, jpeg, png
     * - 단일 페이지: pdf, tif, tiff (신분증 OCR에서는 미사용)
     * - 파일 크기: 1 byte ~ 52,428,800 bytes (약 50MB)
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 없거나 비어있습니다.");
        }
        
        // FILE_SIZE_VALIDATION: 파일 크기 검증 (NCP OCR API 제한)
        long fileSize = file.getSize();
        if (fileSize < MIN_FILE_SIZE) {
            throw new IllegalArgumentException("파일 크기가 너무 작습니다. 최소 1 byte 이상이어야 합니다.");
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                String.format("파일 크기가 너무 큽니다. 최대 %d bytes (약 50MB) 이하여야 합니다.", MAX_FILE_SIZE)
            );
        }
        
        // FILENAME_VALIDATION: 파일명 검증
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("파일명을 확인할 수 없습니다.");
        }
        
        // IMAGE_TYPE_VALIDATION: 확장자 기반 이미지 타입 검증
        String imageFormat = extractImageFormat(filename);
        boolean isValidFormat = false;
        for (String allowedType : ALLOWED_IMAGE_TYPES) {
            if (allowedType.equalsIgnoreCase(imageFormat)) {
                isValidFormat = true;
                break;
            }
        }
        
        if (!isValidFormat) {
            throw new IllegalArgumentException(
                String.format("지원하지 않는 이미지 형식입니다. 허용된 형식: %s", 
                    String.join(", ", ALLOWED_IMAGE_TYPES))
            );
        }
        
        // CONTENT_TYPE_VALIDATION: Content-Type 검증 (추가 안전장치)
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            boolean isValidContentType = false;
            // Content-Type에서 이미지 타입 확인 (image/jpeg, image/jpg, image/png)
            if (contentType.startsWith("image/")) {
                String type = contentType.substring(6).toLowerCase(); // "image/" 제거
                if ("jpeg".equals(type) || "jpg".equals(type) || "png".equals(type)) {
                    isValidContentType = true;
                }
            }
            
            // 확장자는 유효하지만 Content-Type이 다른 경우 경고 (차단하지 않음)
            // 일부 브라우저/클라이언트에서 Content-Type이 부정확할 수 있음
            if (!isValidContentType) {
                log.warn("파일 확장자와 Content-Type이 일치하지 않습니다. 확장자: {}, Content-Type: {}", 
                    imageFormat, contentType);
            }
        }
    }

    /**
     * 파일명에서 이미지 형식 추출
     */
    private String extractImageFormat(String filename) {
        if (filename == null || filename.isBlank()) {
            return "png"; // 기본값
        }
        
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "png"; // 기본값
        }
        
        String extension = filename.substring(lastDotIndex + 1).toLowerCase();
        // jpg와 jpeg를 jpeg로 통일 (NCP OCR API는 jpeg를 요구할 수 있음)
        if ("jpg".equals(extension)) {
            return "jpeg";
        }
        return extension;
    }

    /**
     * NCP OCR API 에러 응답 파싱 및 사용자 친화적 메시지 변환
     * 
     * NCP OCR API 에러 코드 참고:
     * - 0001: URL is invalid
     * - 0002: Secret key validate failed (401)
     * - 0011: Request body invalid (400)
     * - 0021: Protocol version not support
     * - 0022: Request domain invalid
     * - 0023: API request count reach the upper limit
     * - 0025: Calls exceeded the rate limit
     * - 0500: Unknown service error (500)
     * - 0501: OCR service error (500)
     * - 1022: Service is blocked
     */
    private String parseOcrApiError(String errorResponseBody) {
        if (errorResponseBody == null || errorResponseBody.isBlank()) {
            return "OCR API 요청에 실패했습니다.";
        }
        
        try {
            JsonNode errorJson = objectMapper.readTree(errorResponseBody);
            
            // NCP OCR API 에러 응답 구조: {"code":"0011","message":"...", ...}
            String errorCode = errorJson.path("code").asText("");
            String errorMessage = errorJson.path("message").asText("");
            
            // ERROR_CODE_MAPPING: 에러 코드별 사용자 친화적 메시지
            switch (errorCode) {
                case "0001":
                    return "OCR API 호출 URL이 유효하지 않습니다.";
                    
                case "0002":
                    return "OCR API 인증에 실패했습니다. Secret Key를 확인해주세요.";
                    
                case "0011":
                    // Request body invalid - 상세 메시지 분석
                    if (errorMessage.contains("size must be between")) {
                        return "파일 크기가 올바르지 않습니다. 파일 크기는 1 byte 이상 50MB 이하여야 합니다.";
                    } else if (errorMessage.contains("format") || errorMessage.contains("images[0]")) {
                        return "이미지 형식이 올바르지 않습니다. jpg, jpeg, png 형식만 지원됩니다.";
                    } else if (errorMessage.contains("data")) {
                        return "이미지 데이터가 올바르지 않습니다. 파일을 다시 확인해주세요.";
                    }
                    return "OCR 요청 형식이 올바르지 않습니다: " + errorMessage;
                    
                case "0021":
                    return "OCR API 버전이 지원되지 않습니다.";
                    
                case "0022":
                    return "OCR API 요청 도메인이 올바르지 않습니다.";
                    
                case "0023":
                    return "OCR API 호출 한도에 도달했습니다. 잠시 후 다시 시도해주세요.";
                    
                case "0025":
                    return "OCR API 호출 수가 요금 제한을 초과했습니다. 잠시 후 다시 시도해주세요.";
                    
                case "0028":
                    return "표 추출 기능이 활성화되지 않았습니다.";
                    
                case "0500":
                    return "OCR 서비스에서 알 수 없는 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                    
                case "0501":
                    return "OCR 서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
                    
                case "1021":
                    return "OCR 템플릿 배포 정보를 찾을 수 없습니다.";
                    
                case "1022":
                    return "OCR 서비스가 일시 정지되었습니다. 관리자에게 문의해주세요.";
                    
                default:
                    // 알 수 없는 에러 코드이지만 메시지가 있는 경우
                    if (errorMessage != null && !errorMessage.isBlank()) {
                        return "OCR 처리 실패: " + errorMessage + " (코드: " + errorCode + ")";
                    }
                    return "OCR API 요청에 실패했습니다. (코드: " + errorCode + ")";
            }
            
        } catch (Exception e) {
            // JSON 파싱 실패 시 원본 에러 메시지 반환
            log.warn("OCR API 에러 응답 파싱 실패: {}", errorResponseBody);
            return "OCR API 요청에 실패했습니다: " + errorResponseBody;
        }
    }

    /**
     * 신분증 필드 검증
     */
    private void validateIdCardFields(String name, String registrationNumber, String address) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 추출할 수 없습니다.");
        }

        if (name.length() < 2 || name.length() > 10) {
            throw new IllegalArgumentException("이름 형식이 올바르지 않습니다: " + name);
        }

        if (registrationNumber == null || registrationNumber.isBlank()) {
            throw new IllegalArgumentException("주민등록번호를 추출할 수 없습니다.");
        }

        if (!REGISTRATION_NUMBER_PATTERN.matcher(registrationNumber).matches()) {
            log.warn("⚠️ 주민등록번호 형식이 표준과 다릅니다: {}", registrationNumber);
            // 형식이 맞지 않아도 경고만 하고 진행 (OCR 오인식 가능)
        }

        if (address == null || address.isBlank()) {
            log.warn("⚠️ 주소를 추출할 수 없습니다.");
            // 주소는 필수가 아닐 수 있으므로 경고만
        }
    }

    /**
     * 신분증 정보 DTO (이름, 생년월일만 포함)
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class IdCardInfoDto {
        /** 이름 */
        private final String name;
        
        /** 생년월일 (YYYY-MM-DD 형식) */
        private final String birthDate;
        
        /** 원본 OCR 응답 데이터 (디버깅용) */
        private final JsonNode rawData;

        /**
         * Map으로 변환 (API 응답용)
         */
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("name", name);
            map.put("birthDate", birthDate);
            return map;
        }
    }
}
