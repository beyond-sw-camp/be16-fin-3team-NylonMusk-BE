package com.beyond.MKX.domain.assets.service;

import com.beyond.MKX.domain.assets.dto.StockHoldingResDTO;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class StockHoldingService {
    private final StockHoldingRepository stockHoldingRepository;

//    보유 주식 단건 조회
    public StockHoldingResDTO getMyStock(UUID memberAccountId, String ticker) {
        StockHolding stockHolding = stockHoldingRepository.findByMemberAccountIdAndTicker(memberAccountId, ticker)
                .orElseThrow(() -> new IllegalArgumentException("보유 종목이 없습니다."));
        return StockHoldingResDTO.from(stockHolding);
    }

//    보유 주식 전체 조회
    public List<StockHoldingResDTO> getMyStocks(UUID memberAccountId) {
         List<StockHolding> stockHoldingList = stockHoldingRepository.findByAllMemberAccount(memberAccountId);
        if (stockHoldingList.isEmpty()) {
            throw new IllegalArgumentException("해당 게좌의 보유주식이 없습니다.");
        }
        return stockHoldingList.stream().map(StockHoldingResDTO::from).toList();
    }
}
