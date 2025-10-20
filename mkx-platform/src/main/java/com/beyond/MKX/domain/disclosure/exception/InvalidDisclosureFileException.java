package com.beyond.MKX.domain.disclosure.exception;

public class InvalidDisclosureFileException extends RuntimeException {
    public InvalidDisclosureFileException(String message) {
        super(message);
    }
    public InvalidDisclosureFileException(String message, Throwable cause) {
        super(message, cause);
    }
}

