package com.beyond.MKX.domain.order.repository;

import com.beyond.MKX.domain.order.entity.OrderLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface OrderLogRepository extends JpaRepository<OrderLog, UUID> {

    Optional<OrderLog> findByIdAndAccount_Id(UUID orderLogId, UUID memberAccountId);

    /**
     * 증권사별 최근 주문 목록 조회
     */
    Page<OrderLog> findByBrokerageIdOrderByCreatedAtDesc(UUID brokerageId, Pageable pageable);

    /**
     * 증권사별 오늘 주문한 고유 회원 계좌 수 조회
     * 오늘(00:00 ~ 23:59:59) 안에 주문을 넣은 회원 계좌 수를 카운트
     * 
     * @param brokerageId 증권사 ID
     * @param startDateTime 오늘 00:00:00
     * @param endDateTime 오늘 23:59:59
     * @return 오늘 주문한 고유 회원 계좌 수
     */
    @Query("SELECT COUNT(DISTINCT ol.account.id) FROM OrderLog ol " +
           "WHERE ol.brokerageId = :brokerageId " +
           "AND ol.createdAt >= :startDateTime AND ol.createdAt < :endDateTime")
    Long countDistinctMemberAccountsByBrokerageIdAndDateRange(@Param("brokerageId") UUID brokerageId,
                                                               @Param("startDateTime") LocalDateTime startDateTime,
                                                               @Param("endDateTime") LocalDateTime endDateTime);
}
