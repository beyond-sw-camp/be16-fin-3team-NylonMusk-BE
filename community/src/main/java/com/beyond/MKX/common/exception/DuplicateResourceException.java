package com.beyond.MKX.common.exception;


import lombok.Getter;

@Getter
public class DuplicateResourceException extends RuntimeException{

    private final Object data;

    public DuplicateResourceException(String errorMessage) {
        super(errorMessage);
        this.data = null;
    }

    public DuplicateResourceException(String errorMessage, Object data) {
        super(errorMessage);
        this.data = data;
    }
}
