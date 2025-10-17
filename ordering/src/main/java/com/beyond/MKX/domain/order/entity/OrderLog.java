package com.beyond.MKX.domain.order.entity;

import com.beyond.MKX.common.domain.BaseIdAndTimeEntity;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(
        name = "order_log",
        indexes = {
                @Index(name = "ix_orders_member_account_id", columnList = "member_account_id"),
//                @Index(name = "ix_order_log_symbol_status", columnList = "symbol,status"),
//                @Index(name = "ix_order_log_created_at", columnList = "created_at")
        }
)
@SQLDelete(sql = "update member_account set deleted_at = now() where id = ?")
@SQLRestriction("DELETED_AT IS NULL")
public class OrderLog extends BaseIdAndTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_account_id", nullable = false)
    @Comment("주문을 발행한 계좌")
    private MemberAccount account;

    @Column(nullable = false)
    private UUID brokerageId;

    @Column(nullable = false, length = 6)
    private String ticker;

    // 주문 종류(시장가/지정가/예약) – 주문 방식과 체결 정책을 분리하기 위해 별도 필드
    @Enumerated(EnumType.STRING)
    @Column(name = "order_kind", nullable = false, length = 16)
    private OrderKind orderKind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Side side; // BUY/SELL

    // 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    // 지정가 가격(틱 단위 정수). 시장가면 null
    private Long price;

    // 수량(주 수)
    @Column(nullable = false)
    private Long quantity;

    // 수수료
    @Column(nullable = false)
    private Long commission;

    // 매도 시 거래세
    private Long transactionTax;

    // 매수 동결 금액 = 대금 + 수수료
    private Long freezeAmount;

    // 주문 시 동결금액 (SNAP 용도)
    private Long holdAmount;

    // 잔여 수량
    @Column(nullable = false)
    private Long remainQuantity;

    @Column(name = "filled_at")
    private LocalDateTime filledAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    // 낙관적 락
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public void updateFilledAt() {
        this.filledAt = LocalDateTime.now();
    }

    public void decFreezeAmount(Long decAmount) {
        this.freezeAmount -= decAmount;
    }

    public long decRemainQuantity(long decQuantity) {
        this.remainQuantity -= decQuantity;
        if (remainQuantity < 0) {
            throw new IllegalArgumentException("잔여 수량이 0 미만입니다.");
        }

        return this.remainQuantity;
    }

    public void updateOrderStatus(OrderStatus orderStatus) {
        this.status = orderStatus;
    }

    public void insertOrderFreezeAmount(Long freezeAmount) {
        this.freezeAmount = freezeAmount;
        this.holdAmount = freezeAmount;
    }

}
