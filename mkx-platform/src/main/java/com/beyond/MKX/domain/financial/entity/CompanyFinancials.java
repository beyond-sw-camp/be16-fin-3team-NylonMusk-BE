package com.beyond.MKX.domain.financial.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "company_financials",
    uniqueConstraints = @UniqueConstraint(name = "uk_financials_stock_year_quarter",
        columnNames = {"stock_id","fiscal_year","fiscal_quarter"}),
    indexes = @Index(name = "ix_financials_stock_id", columnList = "stock_id")
)
public class CompanyFinancials extends BaseIdAndTimeEntity {

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter; // null 허용

    @Column(name = "revenue")          private Long revenue;
    @Column(name = "operating_income") private Long operatingIncome;
    @Column(name = "net_income")       private Long netIncome;

    @Column(name = "eps", precision = 18, scale = 4)
    private BigDecimal eps;

    @Column(name = "total_assets")      private Long totalAssets;
    @Column(name = "total_liabilities") private Long totalLiabilities;
    @Column(name = "total_equity")      private Long totalEquity;

    public void updateFrom(CompanyFinancials u) {
        this.revenue = u.revenue;
        this.operatingIncome = u.operatingIncome;
        this.netIncome = u.netIncome;
        this.eps = u.eps;
        this.totalAssets = u.totalAssets;
        this.totalLiabilities = u.totalLiabilities;
        this.totalEquity = u.totalEquity;
    }
}
