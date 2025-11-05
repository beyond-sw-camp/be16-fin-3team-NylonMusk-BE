package com.beyond.MKX.common.apiResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ApiResponse {

    public static ResponseEntity<?> ok(Object data) {
        return new ResponseEntity<>(new CommonDTO(
                "success",
                HttpStatus.OK.value(),
                data
        ), HttpStatus.OK);
    }

    public static ResponseEntity<?> ok(Object data, String message) {
        return new ResponseEntity<>(new CommonDTO(
                message,
                HttpStatus.OK.value(),
                data
        ), HttpStatus.OK);
    }

    public static ResponseEntity<?> created(Object data) {
        return new ResponseEntity<>(new CommonDTO(
                "created success",
                HttpStatus.CREATED.value(),
                data
        ), HttpStatus.CREATED);
    }

    public static ResponseEntity<?> created(Object data, String message) {
        return new ResponseEntity<>(new CommonDTO(
                message,
                HttpStatus.CREATED.value(),
                data
        ), HttpStatus.CREATED);
    }

    public static ResponseEntity<?> noContent(Object data) {
        return new ResponseEntity<>(new CommonDTO(
                "no content",
                HttpStatus.NO_CONTENT.value(),
                data
        ), HttpStatus.NO_CONTENT);
    }

    public static ResponseEntity<?> noContent(Object data, String message) {
        return new ResponseEntity<>(new CommonDTO(
                message,
                HttpStatus.NO_CONTENT.value(),
                data
        ), HttpStatus.NO_CONTENT);
    }

    /**
     * ERROR: 에러 응답 생성
     * @param status HTTP 상태 코드
     * @param message 에러 메시지
     * @param data 추가 데이터 (null 가능)
     * @return ResponseEntity
     */
    public static ResponseEntity<?> error(HttpStatus status, String message, Object data) {
        return new ResponseEntity<>(new CommonDTO(
                message,
                status.value(),
                data
        ), status);
    }
}





