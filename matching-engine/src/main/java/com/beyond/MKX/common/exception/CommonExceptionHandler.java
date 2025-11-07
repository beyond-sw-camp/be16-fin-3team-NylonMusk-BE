package com.beyond.MKX.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import java.util.NoSuchElementException;

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

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> authenticationException(AuthenticationException e) {
        log.error("[AuthenticationException] code = {}, message = {}", HttpStatus.FORBIDDEN, e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CommonErrorDTO.builder()
                        .status_message(e.getMessage())
                        .status_code(HttpStatus.FORBIDDEN.value())
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

}
