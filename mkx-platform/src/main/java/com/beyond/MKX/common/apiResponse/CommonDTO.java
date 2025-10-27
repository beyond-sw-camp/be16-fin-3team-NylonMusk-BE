package com.beyond.MKX.common.apiResponse;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
@Data
public class CommonDTO<T> {
    private String status_message;
    private Integer status_code;
    private T result;
}
