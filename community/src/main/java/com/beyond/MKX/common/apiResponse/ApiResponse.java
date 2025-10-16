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

    public static ResponseEntity<?> created(Object data, java.net.URI location, String message) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(location)
                .body(new CommonDTO(message, HttpStatus.CREATED.value(), data));
    }

    // 204 바디 없는 형태
    public static ResponseEntity<?> noContent() {
        return ResponseEntity.noContent().build();
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

    /** 401 Unauthorized */
    public static ResponseEntity<?> unauthorized(String message) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new CommonDTO(message, HttpStatus.UNAUTHORIZED.value(), null));
    }

    /** 403 Forbidden (권한 부족 시 사용) */
    public static ResponseEntity<?> forbidden(String message) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new CommonDTO(message, HttpStatus.FORBIDDEN.value(), null));
    }
}





