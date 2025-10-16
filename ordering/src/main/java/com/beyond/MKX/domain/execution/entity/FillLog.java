package com.beyond.MKX.domain.execution.entity;


import com.beyond.MKX.common.domain.BaseTimeEntity;
import com.beyond.MKX.domain.order.entity.Side;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "fill_log",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_fill_log_order_exec",
                        columnNames = {"order_log_id", "exec_id"}
                )
        }
)
public class FillLog extends BaseTimeEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private UUID orderLogId;

    @Column(nullable = false)
    private String execId;

    @Column(nullable = false, columnDefinition = "VARCHAR(6)")
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Side side;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private long quantity;

    @Column(nullable = false)
    private long brokerageCommission;

}
