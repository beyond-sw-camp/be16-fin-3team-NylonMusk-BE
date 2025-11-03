package com.beyond.MKX.domain.execution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 페이징된 체결 데이터 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedExecutionResponse {
    
    /**
     * 체결 데이터 리스트
     */
    private List<ExecutionEventDTO> content;
    
    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int page;
    
    /**
     * 페이지 당 데이터 개수
     */
    private int size;
    
    /**
     * 전체 데이터 개수
     */
    private long totalElements;
    
    /**
     * 전체 페이지 수
     */
    private int totalPages;
    
    /**
     * 첫 페이지 여부
     */
    private boolean first;
    
    /**
     * 마지막 페이지 여부
     */
    private boolean last;
    
    /**
     * 다음 페이지 존재 여부
     */
    private boolean hasNext;
    
    /**
     * 이전 페이지 존재 여부
     */
    private boolean hasPrevious;
    
    /**
     * 비어있는 페이지 여부
     */
    private boolean empty;
}
