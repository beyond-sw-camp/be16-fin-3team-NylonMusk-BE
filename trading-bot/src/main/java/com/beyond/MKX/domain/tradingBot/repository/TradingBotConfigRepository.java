package com.beyond.MKX.domain.tradingBot.repository;

import com.beyond.MKX.domain.tradingBot.entity.TradingBotConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TradingBotConfigRepository extends JpaRepository<TradingBotConfig, UUID> {
    
    /**
     * 활성화된 모든 봇 설정 조회
     */
    List<TradingBotConfig> findByIsActiveTrue();
    
    /**
     * 특정 상태의 활성화된 봇 설정 조회
     */
    List<TradingBotConfig> findByStatusAndIsActiveTrue(String status);
    
    /**
     * 특정 종목의 활성화된 봇 설정 조회
     */
    List<TradingBotConfig> findByTickerAndIsActiveTrue(String ticker);
    
    /**
     * 특정 증권사의 활성화된 봇 설정 조회
     */
    List<TradingBotConfig> findByBrokerageIdAndIsActiveTrue(String brokerageId);
    
    /**
     * 특정 종목과 상태의 활성화된 봇 설정 조회
     */
    @Query("SELECT t FROM TradingBotConfig t WHERE t.ticker = :ticker AND t.status = :status AND t.isActive = true")
    List<TradingBotConfig> findByTickerAndStatusAndIsActiveTrue(@Param("ticker") String ticker, @Param("status") String status);
    
    /**
     * 특정 봇 설정을 비활성화
     */
    @Query("UPDATE TradingBotConfig t SET t.isActive = false WHERE t.id = :id")
    void deactivateById(@Param("id") UUID id);
}
