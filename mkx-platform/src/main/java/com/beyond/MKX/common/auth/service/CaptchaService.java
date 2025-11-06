package com.beyond.MKX.common.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * CAPTCHA_SERVICE: NCP CAPTCHA 검증 서비스
 * - 이미지 CAPTCHA 검증
 * - 음성 CAPTCHA 검증
 */
@Slf4j
@Service
public class CaptchaService {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String secretKey;
    private final String apiUrl;
    private final String imageBinUrl;
    private final String audioApiUrl;
    private final String audioBinUrl;

    public CaptchaService(
            RestTemplate restTemplate,
            @Value("${ncp.captcha.client-id}") String clientId,
            @Value("${ncp.captcha.secret-key}") String secretKey,
            @Value("${ncp.captcha.api-url}") String apiUrl,
            @Value("${ncp.captcha.image-bin-url}") String imageBinUrl,
            @Value("${ncp.captcha.audio-api-url}") String audioApiUrl,
            @Value("${ncp.captcha.audio-bin-url}") String audioBinUrl
    ) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.secretKey = secretKey;
        this.apiUrl = apiUrl;
        this.imageBinUrl = imageBinUrl;
        this.audioApiUrl = audioApiUrl;
        this.audioBinUrl = audioBinUrl;
    }

    /**
     * GENERATE_CAPTCHA_KEY: CAPTCHA 키 생성 (이미지용)
     * NCP CAPTCHA API: GET /captcha/v1/nkey?code=0 (키 발급)
     * @return CAPTCHA 키 (nkey)
     */
    public String generateCaptchaKey() {
        try {
            // 이미지 CAPTCHA 키 발급: code=0
            String generateUrl = apiUrl + "?code=0";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", secretKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.debug("CAPTCHA 키 생성 API 호출: url={}, clientId={}", generateUrl, clientId);
            
            // 응답을 String으로 받아서 JSON 파싱
            ResponseEntity<String> response = restTemplate.exchange(
                    generateUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            log.info("CAPTCHA 키 생성 API 응답: status={}, headers={}, body={}", 
                response.getStatusCode(), 
                response.getHeaders().getContentType(),
                response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody().trim();
                log.info("CAPTCHA 키 생성 API 응답 본문 (원문): {}", responseBody);
                
                // JSON 파싱
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
                    log.info("CAPTCHA 키 생성 API 응답 파싱 결과: {}", body);
                    
                    if (body.containsKey("key")) {
                        String key = (String) body.get("key");
                        log.info("CAPTCHA 키 생성 성공: key={}", key);
                        return key;
                    }
                    
                    log.warn("CAPTCHA 키 생성 API 응답에 'key' 필드가 없음: body={}", body);
                    return null;
                } catch (Exception e) {
                    log.error("CAPTCHA 키 응답 JSON 파싱 실패: responseBody={}, error={}", responseBody, e.getMessage(), e);
                    return null;
                }
            }
            
            log.warn("CAPTCHA 키 생성 API 응답 오류: status={}, body={}", response.getStatusCode(), response.getBody());
            return null;
            
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("CAPTCHA 키 생성 HTTP 클라이언트 에러: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("CAPTCHA 키 생성 HTTP 서버 에러: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString(), e);
            return null;
        } catch (Exception e) {
            log.error("CAPTCHA 키 생성 실패: exception={}, message={}", e.getClass().getName(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * GENERATE_AUDIO_CAPTCHA_KEY: 음성 CAPTCHA 키 생성
     * NCP CAPTCHA API: GET /scaptcha/v1/skey?code=0 (키 발급)
     * @return 음성 CAPTCHA 키 (skey)
     */
    public String generateAudioCaptchaKey() {
        try {
            // 음성 CAPTCHA 키 발급: code=0
            String generateUrl = audioApiUrl + "?code=0";
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", secretKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            log.debug("음성 CAPTCHA 키 생성 API 호출: url={}", generateUrl);
            
            // 응답을 String으로 받아서 JSON 파싱
            ResponseEntity<String> response = restTemplate.exchange(
                    generateUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody().trim();
                
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
                    
                    if (body.containsKey("key")) {
                        String key = (String) body.get("key");
                        log.info("음성 CAPTCHA 키 생성 성공: key={}", key);
                        return key;
                    }
                } catch (Exception e) {
                    log.error("음성 CAPTCHA 키 응답 JSON 파싱 실패: responseBody={}", responseBody, e);
                    return null;
                }
            }
            
            log.warn("음성 CAPTCHA 키 생성 API 응답 오류: status={}, body={}", response.getStatusCode(), response.getBody());
            return null;
            
        } catch (Exception e) {
            log.error("음성 CAPTCHA 키 생성 실패", e);
            return null;
        }
    }

    /**
     * GET_CAPTCHA_IMAGE_URL: CAPTCHA 이미지 URL 생성
     * 이미지 파일 요청: /captcha-bin/v1/nkey?key=KEY (문서 기준)
     * @param captchaKey CAPTCHA 키
     * @return CAPTCHA 이미지 URL
     */
    public String getCaptchaImageUrl(String captchaKey) {
        return imageBinUrl + "?key=" + captchaKey;
    }

    /**
     * GET_CAPTCHA_AUDIO_URL: CAPTCHA 음성 URL 생성
     * 음성 CAPTCHA: 별도 키(skey) 사용
     * @param audioCaptchaKey 음성 CAPTCHA 키 (skey)
     * @return CAPTCHA 음성 URL
     */
    public String getCaptchaAudioUrl(String audioCaptchaKey) {
        return audioBinUrl + "?key=" + audioCaptchaKey + "&X-NCP-APIGW-API-KEY-ID=" + clientId;
    }

    /**
     * VERIFY_CAPTCHA: CAPTCHA 검증 (이미지)
     * NCP CAPTCHA 검증 API: GET /captcha/v1/nkey?code=1&key={key}&value={value}
     * @param captchaKey CAPTCHA 키 (nkey)
     * @param captchaValue CAPTCHA 값 (사용자 입력값)
     * @return 검증 성공 여부
     */
    public boolean verifyCaptcha(String captchaKey, String captchaValue) {
        try {
            // 이미지 CAPTCHA 검증: code=1
            String verifyUrl = apiUrl + "?code=1&key=" + captchaKey + "&value=" + captchaValue;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", secretKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 응답을 String으로 받아서 JSON 파싱
            ResponseEntity<String> response = restTemplate.exchange(
                    verifyUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            // 응답 확인
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody().trim();
                
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
                    
                    // NCP CAPTCHA API는 성공 시 "result": true를 반환
                    Object resultObj = body.get("result");
                    boolean isValid = false;
                    
                    if (resultObj instanceof Boolean) {
                        isValid = (Boolean) resultObj;
                    } else if (resultObj instanceof String) {
                        isValid = Boolean.parseBoolean((String) resultObj);
                    } else if (resultObj != null) {
                        isValid = Boolean.parseBoolean(resultObj.toString());
                    }
                    
                    log.info("이미지 CAPTCHA 검증 결과: key={}, valid={}", captchaKey, isValid);
                    return isValid;
                } catch (Exception e) {
                    log.error("CAPTCHA 검증 응답 JSON 파싱 실패: responseBody={}", responseBody, e);
                    return false;
                }
            }
            
            log.warn("CAPTCHA 검증 API 응답 오류: status={}, body={}", response.getStatusCode(), response.getBody());
            return false;
            
        } catch (Exception e) {
            log.error("CAPTCHA 검증 실패: key={}", captchaKey, e);
            return false;
        }
    }

    /**
     * VERIFY_AUDIO_CAPTCHA: CAPTCHA 검증 (음성)
     * NCP 음성 CAPTCHA 검증 API: GET /scaptcha/v1/skey?code=1&key={key}&value={value}
     * @param audioCaptchaKey 음성 CAPTCHA 키 (skey)
     * @param captchaValue CAPTCHA 값 (사용자 입력값)
     * @return 검증 성공 여부
     */
    public boolean verifyAudioCaptcha(String audioCaptchaKey, String captchaValue) {
        try {
            // 음성 CAPTCHA 검증: code=1
            String verifyUrl = audioApiUrl + "?code=1&key=" + audioCaptchaKey + "&value=" + captchaValue;
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-NCP-APIGW-API-KEY-ID", clientId);
            headers.set("X-NCP-APIGW-API-KEY", secretKey);
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 응답을 String으로 받아서 JSON 파싱
            ResponseEntity<String> response = restTemplate.exchange(
                    verifyUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            // 응답 확인
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                String responseBody = response.getBody().trim();
                
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> body = objectMapper.readValue(responseBody, Map.class);
                    
                    // NCP CAPTCHA API는 성공 시 "result": true를 반환
                    Object resultObj = body.get("result");
                    boolean isValid = false;
                    
                    if (resultObj instanceof Boolean) {
                        isValid = (Boolean) resultObj;
                    } else if (resultObj instanceof String) {
                        isValid = Boolean.parseBoolean((String) resultObj);
                    } else if (resultObj != null) {
                        isValid = Boolean.parseBoolean(resultObj.toString());
                    }
                    
                    log.info("음성 CAPTCHA 검증 결과: key={}, valid={}", audioCaptchaKey, isValid);
                    return isValid;
                } catch (Exception e) {
                    log.error("음성 CAPTCHA 검증 응답 JSON 파싱 실패: responseBody={}", responseBody, e);
                    return false;
                }
            }
            
            log.warn("음성 CAPTCHA 검증 API 응답 오류: status={}, body={}", response.getStatusCode(), response.getBody());
            return false;
            
        } catch (Exception e) {
            log.error("음성 CAPTCHA 검증 실패: key={}", audioCaptchaKey, e);
            return false;
        }
    }
}

