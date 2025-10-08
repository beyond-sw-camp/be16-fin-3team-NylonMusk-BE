package com.beyond.MKX.domain.assets.repository;

import com.beyond.MKX.domain.assets.entity.StockHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StockHoldingRepository extends JpaRepository<StockHolding, UUID> {

    Optional<StockHolding> findByMemberAccountIdAndTicker(UUID memberAccountId, String ticker);
}
