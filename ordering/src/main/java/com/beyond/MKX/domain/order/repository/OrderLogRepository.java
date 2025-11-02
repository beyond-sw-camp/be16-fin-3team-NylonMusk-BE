package com.beyond.MKX.domain.order.repository;

import com.beyond.MKX.domain.order.entity.OrderLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderLogRepository extends JpaRepository<OrderLog, UUID> {

    Optional<OrderLog> findByIdAndAccount_Id(UUID orderLogId, UUID memberAccountId);

    /**
     * 증권사별 최근 주문 목록 조회
     */
    Page<OrderLog> findByBrokerageIdOrderByCreatedAtDesc(UUID brokerageId, Pageable pageable);

}
