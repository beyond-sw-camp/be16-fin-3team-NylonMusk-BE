package com.beyond.MKX.domain.assets.repository;

import com.beyond.MKX.domain.assets.entity.StockHolding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockHoldingRepository extends JpaRepository<StockHolding, UUID> {

    Optional<StockHolding> findByMemberAccountIdAndTicker(UUID memberAccountId, String ticker);

    /**
     * 계좌 ID와 종목 코드로 보유 주식 조회 (비관적 잠금 사용)
     * 동시 주문 처리 시 DB 충돌 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sh FROM StockHolding sh WHERE sh.memberAccountId = :memberAccountId AND sh.ticker = :ticker")
    Optional<StockHolding> findByMemberAccountIdAndTickerWithLock(@Param("memberAccountId") UUID memberAccountId, @Param("ticker") String ticker);

    List<StockHolding> findAllByMemberAccountId(UUID memberAccountId);

    /**
     * 특정 ticker의 모든 보유자 조회 (내부 API용)
     */
    @Query("SELECT sh FROM StockHolding sh WHERE sh.ticker = :ticker AND sh.totalQuantity > 0")
    List<StockHolding> findAllByTicker(@Param("ticker") String ticker);

    /**
     * 증권사별 보유 종목 조회
     */
    List<StockHolding> findAllByBrokerageId(UUID brokerageId);
}