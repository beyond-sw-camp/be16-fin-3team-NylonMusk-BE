package com.beyond.MKX.domain.stockfavorite.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.member.entity.Member;
import com.beyond.MKX.domain.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@Table(name = "stock_favorites",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_member_stock", columnNames = {"member_id", "stock_id"}))
// 유니크 제약 조건 (uk_member_stock)을 걸어 한 회원이 같은 종목을 즐겨찾기 할 수 없게 된다
public class StockFavorites extends BaseIdAndTimeEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;
}
