package com.beyond.MKX.domain.financial.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
    name = "cash_flow_statement",
    uniqueConstraints = @UniqueConstraint(name = "uk_cfs_stock_year_quarter",
        columnNames = {"stock_id","fiscal_year","fiscal_quarter"}),
    indexes = @Index(name = "ix_cfs_stock_id", columnList = "stock_id")
)
public class CashFlowStatement extends BaseIdAndTimeEntity {

    @Column(name = "stock_id", nullable = false)
    private UUID stockId;

    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "fiscal_quarter")
    private Integer fiscalQuarter;

    @Column(name = "operating_cash_flow") private Long operatingCashFlow;
    @Column(name = "investing_cash_flow") private Long investingCashFlow;
    @Column(name = "financing_cash_flow") private Long financingCashFlow;
    @Column(name = "free_cash_flow")      private Long freeCashFlow;

    public void updateFrom(CashFlowStatement u) {
        this.operatingCashFlow = u.operatingCashFlow;
        this.investingCashFlow = u.investingCashFlow;
        this.financingCashFlow = u.financingCashFlow;
        this.freeCashFlow = u.freeCashFlow;
    }
}
