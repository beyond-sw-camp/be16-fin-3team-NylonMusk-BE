package com.beyond.MKX.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.nio.file.AccessDeniedException;
import java.util.NoSuchElementException;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class CommonExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalException(IllegalArgumentException e) {
        log.error("[IllegalArgumentException] code = {}, message = {}", HttpStatus.BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.BAD_REQUEST.value())
                        .build()

                );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> entityException(EntityNotFoundException e) {
        log.error("[EntityNotFoundException] code = {}, message = {}", HttpStatus.NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.NOT_FOUND.value())
                        .build()
                );
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> NoSuchElementException(NoSuchElementException e) {
        log.error("[NoSuchElementException] code = {}, message = {}", HttpStatus.NOT_FOUND, e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.NOT_FOUND.value())
                        .build()
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> MethodNotValidException(MethodArgumentNotValidException e) {
        log.error("[MethodArgumentNotValidException] code = {}, message = {}", HttpStatus.BAD_REQUEST, e.getMessage());
        String defaultMessage = e.getBindingResult().getFieldError().getDefaultMessage();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonErrorDTO.builder()
                        .status_message(defaultMessage)
                        .status_code(HttpStatus.BAD_REQUEST.value())
                        .build()
                );

    }

    // 403 Forbidden: 인가되지 않음 (권한 없음)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> AccessDeniedException(AccessDeniedException e) {
        log.error("[AccessDeniedException] code = {}, message = {}", HttpStatus.FORBIDDEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.FORBIDDEN.value())
                        .build()
                );
    }

    // 401 Unauthorized: 인증되지 않음 (로그인 필요)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> authorizedException(AuthenticationException e) {
        log.error("[AuthenticationException] code = {}, message = {}", HttpStatus.UNAUTHORIZED, e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.UNAUTHORIZED.value())
                        .build()
                );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<?> duplicateResourceException(DuplicateResourceException e) {
        log.error("[DuplicateResourceException] code = {}, message = {}", HttpStatus.BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.BAD_REQUEST.value())
                        .result(e.getData())
                        .build()
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleInvalidUUID(HttpMessageNotReadableException e) {
        String rootMessage = e.getMessage();

        if (rootMessage != null && rootMessage.contains("UUID")) {
            log.warn("[HttpMessageNotReadableException - UUID] {}", rootMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(CommonErrorDTO.builder()
                            .status_code(HttpStatus.BAD_REQUEST.value())
                            .status_message("요청 본문에 유효하지 않은 UUID가 포함되어 있습니다.")
                            .build());
        }

        // UUID 관련이 아닌 다른 파싱 예외의 경우 일반 메시지 처리
        log.warn("[HttpMessageNotReadableException - General] {}", rootMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonErrorDTO.builder()
                        .status_code(HttpStatus.BAD_REQUEST.value())
                        .status_message("잘못된 요청 형식입니다.")
                        .build());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> illegalStateException(IllegalStateException e) {
        log.error("[IllegalStateException] code = {}, message = {}", HttpStatus.BAD_REQUEST, e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.BAD_REQUEST.value())
                        .build()

                );
    }

    // FeignException 처리: Feign 클라이언트 호출 시 발생하는 예외
    @ExceptionHandler(FeignException.class)
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> handleFeignException(FeignException e) {
        log.error("[FeignException] status = {}, message = {}", e.status(), e.getMessage());
        
        String errorMessage = "서비스 간 통신 중 오류가 발생했습니다.";
        
        try {
            // FeignException의 content()에서 실제 에러 메시지 파싱
            if (e.contentUTF8() != null && !e.contentUTF8().isEmpty()) {
                String content = e.contentUTF8();
                log.debug("[FeignException] content = {}", content);
                
                // JSON 파싱 시도
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    Map<String, Object> errorData = (Map<String, Object>) objectMapper.readValue(content, Map.class);
                    
                    // ordering-service에서 반환한 에러 메시지 추출
                    if (errorData.containsKey("status_message")) {
                        errorMessage = (String) errorData.get("status_message");
                    } else if (errorData.containsKey("message")) {
                        errorMessage = (String) errorData.get("message");
                    }
                } catch (Exception parseException) {
                    // JSON 파싱 실패 시 content를 그대로 사용
                    log.warn("[FeignException] JSON 파싱 실패, content를 그대로 사용: {}", content);
                    errorMessage = content;
                }
            }
        } catch (Exception ex) {
            log.warn("[FeignException] 에러 메시지 추출 실패: {}", ex.getMessage());
        }
        
        // FeignException의 status를 기반으로 HTTP 상태 코드 결정
        // BadRequest(400)인 경우 400으로, 그 외는 원래 상태 코드 사용
        HttpStatus httpStatus = HttpStatus.BAD_REQUEST;
        if (e.status() >= 400 && e.status() < 500) {
            // 400 에러는 그대로 400으로, 401은 400으로 변환 (비즈니스 로직 에러이므로)
            if (e.status() == 400) {
                httpStatus = HttpStatus.BAD_REQUEST;
            } else {
                // 401 등 다른 4xx 에러도 비즈니스 로직 에러일 수 있으므로 400으로 변환
                httpStatus = HttpStatus.BAD_REQUEST;
            }
        } else if (e.status() >= 500) {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        
        log.error("[FeignException] 최종 처리 - status = {}, message = {}", httpStatus.value(), errorMessage);
        
        return ResponseEntity.status(httpStatus)
                .body(CommonErrorDTO.builder()
                        .status_message(errorMessage)
                        .status_code(httpStatus.value())
                        .build()
                );
    }

}
