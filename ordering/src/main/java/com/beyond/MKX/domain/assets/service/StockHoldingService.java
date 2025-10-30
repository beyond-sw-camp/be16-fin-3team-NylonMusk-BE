package com.beyond.MKX.domain.assets.service;

import com.beyond.MKX.domain.assets.dto.AccountIdResDTO;
import com.beyond.MKX.domain.assets.dto.StockHoldingResDTO;
import com.beyond.MKX.domain.assets.dto.StockInfoResDTO;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockHoldingService {
    private final StockHoldingRepository stockHoldingRepository;
    private final AccountFeign accountFeign;
    private final StockFeign stockFeign;

    //    보유 주식 단건 조회
    public StockHoldingResDTO getMyStock(UUID memberAccountId, String ticker) {
        StockHolding stockHolding = stockHoldingRepository.findByMemberAccountIdAndTicker(memberAccountId, ticker)
                .orElseThrow(() -> new IllegalArgumentException("보유 종목이 없습니다."));
        
        // Feign으로 Stock 정보 조회하여 기업명 추가
        StockInfoResDTO stockInfo = stockFeign.getStockByTicker(stockHolding.getTicker());
        
        return StockHoldingResDTO.builder()
                .memberAccountId(stockHolding.getMemberAccountId())
                .brokerageId(stockHolding.getBrokerageId())
                .ticker(stockHolding.getTicker())
                .nameKo(stockInfo.getNameKo()) // mkx-platform의 Stock에서 가져온 기업명
                .totalQuantity(stockHolding.getTotalQuantity())
                .availableQuantity(stockHolding.getAvailableQuantity())
                .totalPurchasePrice(stockHolding.getTotalPurchasePrice())
                .build();
    }

    //    보유 주식 전체 조회
    public List<StockHoldingResDTO> getMyStocks(UUID memberAccountId) {
        List<StockHolding> stockHoldingList = stockHoldingRepository.findAllByMemberAccountId(memberAccountId);

        return stockHoldingList.stream().map(holding -> {
            System.out.println("🔍 StockHoldingService.getMyStocks - 처리 중인 ticker: " + holding.getTicker());
            try {
                // Feign으로 Stock 정보 조회
                System.out.println("🔍 StockFeign 호출 시작 - ticker: " + holding.getTicker());
                StockInfoResDTO stockInfo = stockFeign.getStockByTicker(holding.getTicker());
                System.out.println("✅ StockFeign 호출 성공 - ticker: " + holding.getTicker() + ", nameKo: " + stockInfo.getNameKo());
                
                return StockHoldingResDTO.builder()
                        .memberAccountId(holding.getMemberAccountId())
                        .brokerageId(holding.getBrokerageId())
                        .ticker(holding.getTicker())
                        .nameKo(stockInfo.getNameKo()) // mkx-platform의 Stock에서 가져온 기업명
                        .totalQuantity(holding.getTotalQuantity())
                        .availableQuantity(holding.getAvailableQuantity())
                        .totalPurchasePrice(holding.getTotalPurchasePrice())
                        .build();
            } catch (Exception e) {
                System.err.println("❌ Feign 호출 실패 - ticker: " + holding.getTicker() + ", error: " + e.getMessage());
                e.printStackTrace();
                
                // Feign 호출 실패 시 기본값 사용
                return StockHoldingResDTO.builder()
                        .memberAccountId(holding.getMemberAccountId())
                        .brokerageId(holding.getBrokerageId())
                        .ticker(holding.getTicker())
                        .nameKo("종목명없음") // 실패 시 기본값
                        .totalQuantity(holding.getTotalQuantity())
                        .availableQuantity(holding.getAvailableQuantity())
                        .totalPurchasePrice(holding.getTotalPurchasePrice())
                        .build();
            }
        }).toList();
    }

    /**
     * 특정 ticker의 모든 보유자 조회 (내부 API용)
     */
    public List<StockHoldingResDTO> getAllHoldersByTicker(String ticker) {
        List<StockHolding> holdings = stockHoldingRepository.findAllByTicker(ticker);
        return holdings.stream()
                .map(holding -> StockHoldingResDTO.from(holding))
                .toList();
    }

    /**
     * 특정 ticker의 모든 stock holdings 삭제 (상장폐지용)
     * 
     * @param ticker 주식 티커
     * @return 삭제된 개수
     */
    @org.springframework.transaction.annotation.Transactional
    public int deleteAllByTicker(String ticker) {
        List<StockHolding> holdings = stockHoldingRepository.findAllByTicker(ticker);
        int count = holdings.size();
        
        if (count == 0) {
            return 0;
        }
        
        // Soft delete (SQLDelete 어노테이션으로 자동 처리됨)
        holdings.forEach(holding -> {
            stockHoldingRepository.delete(holding);
            System.out.println("📊 Stock holding 삭제: accountId=" + holding.getMemberAccountId() + 
                    ", ticker=" + ticker + 
                    ", quantity=" + holding.getTotalQuantity());
        });
        
        System.out.println("✅ 상장폐지로 인한 stock holdings 삭제 완료: ticker=" + ticker + ", count=" + count);
        return count;
    }

    public AccountIdResDTO getCorporationAccountId(UUID corpId) {
        return accountFeign.getAccountId(corpId);
    }
}