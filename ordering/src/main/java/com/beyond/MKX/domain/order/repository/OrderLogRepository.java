package com.beyond.MKX.domain.order.repository;

import com.beyond.MKX.domain.order.entity.OrderLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
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

    /**
     * 회원의 대기 중인 주문 조회
     * - 지정가(LIMIT) 또는 예약(RESERVED) 주문
     * - 상태가 PENDING 또는 PARTIALLY_FILLED인 주문
     * 
     * @param memberId 회원 ID
     * @return 대기 중인 주문 목록 (최신순)
     */
    @Query("SELECT ol FROM OrderLog ol " +
           "WHERE ol.account.memberId = :memberId " +
           "AND ol.orderKind IN ('LIMIT', 'RESERVED') " +
           "AND ol.status IN ('PENDING', 'PARTIALLY_FILLED') " +
           "ORDER BY ol.createdAt DESC")
    List<OrderLog> findPendingOrdersByMemberId(@Param("memberId") UUID memberId);

    /**
     * 회원의 대기 중인 주문 조회 (페이징)
     * - 지정가(LIMIT) 또는 예약(RESERVED) 주문
     * - 상태가 PENDING 또는 PARTIALLY_FILLED인 주문
     * 
     * @param memberId 회원 ID
     * @param pageable 페이지 정보
     * @return 대기 중인 주문 목록 (최신순, 페이징)
     */
    @Query("SELECT ol FROM OrderLog ol " +
           "WHERE ol.account.memberId = :memberId " +
           "AND ol.orderKind IN ('LIMIT', 'RESERVED') " +
           "AND ol.status IN ('PENDING', 'PARTIALLY_FILLED') " +
           "ORDER BY ol.createdAt DESC")
    Page<OrderLog> findPendingOrdersByMemberId(@Param("memberId") UUID memberId, Pageable pageable);

    /**
     * 회원의 특정 종목에 대한 주문 로그 조회 (페이징)
     * - 모든 상태의 주문 포함
     * 
     * @param memberId 회원 ID
     * @param ticker 종목 코드
     * @param pageable 페이지 정보
     * @return 주문 로그 목록 (최신순, 페이징)
     */
    @Query("SELECT ol FROM OrderLog ol " +
           "WHERE ol.account.memberId = :memberId " +
           "AND ol.ticker = :ticker " +
           "ORDER BY ol.createdAt DESC")
    Page<OrderLog> findByMemberIdAndTickerOrderByCreatedAtDesc(
            @Param("memberId") UUID memberId,
            @Param("ticker") String ticker,
            Pageable pageable
    );
}
