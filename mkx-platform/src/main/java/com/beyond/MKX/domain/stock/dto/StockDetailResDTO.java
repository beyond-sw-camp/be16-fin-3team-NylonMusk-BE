package com.beyond.MKX.domain.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * STOCK_DETAIL_RES_DTO: 종목 상세 정보 응답 (Corporation 정보 포함)
 * - 프론트엔드 종목 정보 페이지에서 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockDetailResDTO {
    // Stock 정보
    private String ticker;
    private String nameKo;
    private String imageUrl; // 종목 이미지 URL
    private Long totalSharesOutstanding;
    private LocalDateTime listedAt;  // 상장일 (Stock의 createdAt 또는 IPO의 listingAt)
    
    // Corporation 정보
    private String ownerName;  // 대표이사
    private LocalDate estDate;  // 설립일
    private String nameEng;  // 영문명 (옵션)
    
    // 시장 정보
    private Long marketCap;           // 시가총액 (현재가 × 발행주식수)
    private Long enterpriseValue;     // 기업 가치 (시가총액 + 순부채, 1시간마다 업데이트)
}

