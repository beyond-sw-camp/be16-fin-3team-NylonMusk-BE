package com.beyond.MKX.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class CommonErrorDTO {
    private String status_message;
    private Integer status_code;
    private Object result;
}
