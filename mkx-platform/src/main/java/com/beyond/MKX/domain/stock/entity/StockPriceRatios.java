package com.beyond.MKX.domain.stock.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * STOCK_PRICE_RATIOS: 현재가 기반 재무비율 엔티티
 * - 분기/연도와 무관하게 실시간으로 변하는 현재가 기반 비율 저장
 * - PER, PBR, PSR, 시가총액 등
 * - 1시간마다 업데이트 (매 정시)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
    name = "stock_price_ratios",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_price_ratios_stock_id",
        columnNames = {"stock_id"}
    ),
    indexes = @Index(name = "ix_price_ratios_stock_id", columnList = "stock_id")
)
public class StockPriceRatios extends BaseIdAndTimeEntity {

    @Column(name = "stock_id", nullable = false, unique = true)
    private UUID stockId;

    // 현재가 (최신 가격)
    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    // 시가총액 (현재가 × 발행주식수)
    @Column(name = "market_cap")
    private Long marketCap;

    // 기업 가치 (Enterprise Value) = 시가총액 + 순부채
    // 순부채 = 총부채 - 현금 및 현금성 자산 (현금 정보가 없으면 총부채로 근사 계산)
    @Column(name = "enterprise_value")
    private Long enterpriseValue;

    // PER (Price-to-Earnings Ratio) = 현재가 / EPS
    @Column(name = "per", precision = 10, scale = 2)
    private BigDecimal per;

    // PBR (Price-to-Book Ratio) = 현재가 / BPS
    @Column(name = "pbr", precision = 10, scale = 2)
    private BigDecimal pbr;

    // PSR (Price-to-Sales Ratio) = 시가총액 / 매출액
    @Column(name = "psr", precision = 10, scale = 2)
    private BigDecimal psr;
}

