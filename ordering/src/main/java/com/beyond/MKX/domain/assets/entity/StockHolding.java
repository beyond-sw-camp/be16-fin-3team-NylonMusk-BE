package com.beyond.MKX.domain.assets.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "stock_holding",
        indexes = {
                @Index(name = "ix_stock_holding_account_id", columnList = "member_account_id"),
                @Index(name = "ix_stock_holding_brokerage_id", columnList = "brokerage_id")},
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_holding_account_ticker",
                        columnNames = {"member_account_id", "ticker"}
                )
        }
)
@SQLDelete(sql = "update stock_holding set deleted_at = now() where id = ?")
@SQLRestriction("DELETED_AT IS NULL")
public class StockHolding extends BaseIdAndTimeEntity {
    @Column(nullable = false)
    private UUID memberAccountId;

    @Column(nullable = false)
    private UUID brokerageId;

    @Column(nullable = false, columnDefinition = "VARCHAR(6)")
    private String ticker;

    @Column(nullable = false)
    private Long totalQuantity;

    @Column(nullable = false)
    private Long availableQuantity;

//    @Column(nullable = false)
//    @Comment("주식 1주당 취득 원가")
//    private Long averagePrice;

    @Column(nullable = false)
    @Comment("전체 주식의 총 취득 원가")
    private Long totalPurchasePrice;

    public void increaseTotalQuantity(Long incQuantity) {
        this.totalQuantity = Math.addExact(this.totalQuantity, incQuantity);
    }

    public void decreaseTotalQuantity(Long decQuantity) {
        this.totalQuantity -= decQuantity;
        if (this.totalQuantity < 0) {
            throw new IllegalArgumentException("보유주식의 개수가 0 이하일 수 없습니다.");
        }
    }

    public void decreaseAvailableQuantity(Long quantity) {
        this.availableQuantity = Math.subtractExact(this.availableQuantity, quantity);
    }


    public void increaseAvaQuantity(Long incQuantity) {
        this.availableQuantity = Math.addExact(this.availableQuantity, incQuantity);
    }

    public void incTotalPurchasePrice(Long quantity, Long price) {
        long totalAmount = Math.multiplyExact(quantity, price);
        this.totalPurchasePrice = Math.addExact(this.totalPurchasePrice, totalAmount);
    }
    public void decTotalPurchasePrice(Long quantity, Long price) {
        long totalAmount = Math.multiplyExact(quantity, price);
        this.totalPurchasePrice = Math.subtractExact(this.totalPurchasePrice, totalAmount);
    }

}
