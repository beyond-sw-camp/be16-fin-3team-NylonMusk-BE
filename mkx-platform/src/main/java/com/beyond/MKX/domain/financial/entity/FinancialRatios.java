package com.beyond.MKX.domain.financial.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "financial_ratios",
    uniqueConstraints = @UniqueConstraint(name = "uk_ratios_stock_year_quarter",
        columnNames = {"stock_id","fiscal_year","fiscal_quarter"}),
    indexes = @Index(name="ix_ratios_stock_id", columnList = "stock_id")
)
public class FinancialRatios extends BaseIdAndTimeEntity {

    @Column(name="stock_id", nullable=false)
    private UUID stockId;

    @Column(name="fiscal_year", nullable=false)
    private int fiscalYear;

    @Column(name="fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(name="per",               precision=10, scale=2) private BigDecimal per;
    @Column(name="pbr",               precision=10, scale=2) private BigDecimal pbr;
    @Column(name="operating_margin",  precision=10, scale=2) private BigDecimal operatingMargin;
    @Column(name="net_margin",        precision=10, scale=2) private BigDecimal netMargin;
    @Column(name="debt_ratio",        precision=10, scale=2) private BigDecimal debtRatio;
    @Column(name="current_ratio",     precision=10, scale=2) private BigDecimal currentRatio;
    @Column(name="interest_coverage", precision=10, scale=2) private BigDecimal interestCoverage;
    @Column(name="roa",               precision=10, scale=2) private BigDecimal roa;
    @Column(name="roe",               precision=10, scale=2) private BigDecimal roe;

    public void updateFrom(FinancialRatios u) {
        this.per = u.per; this.pbr = u.pbr;
        this.operatingMargin = u.operatingMargin; this.netMargin = u.netMargin;
        this.debtRatio = u.debtRatio; this.currentRatio = u.currentRatio;
        this.interestCoverage = u.interestCoverage; this.roa = u.roa; this.roe = u.roe;
    }
}
