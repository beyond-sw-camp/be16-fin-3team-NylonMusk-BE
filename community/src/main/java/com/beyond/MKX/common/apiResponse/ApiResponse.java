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
}





