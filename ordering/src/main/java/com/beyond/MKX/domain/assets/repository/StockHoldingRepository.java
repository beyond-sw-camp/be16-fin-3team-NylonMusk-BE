package com.beyond.MKX.domain.assets.repository;

import com.beyond.MKX.domain.assets.entity.StockHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.*;

public interface StockHoldingRepository extends JpaRepository<StockHolding, UUID> {

    Optional<StockHolding> findByMemberAccountIdAndTicker(UUID memberAccountId, String ticker);

    List<StockHolding> findAllByMemberAccountId(UUID memberAccountId);
}
