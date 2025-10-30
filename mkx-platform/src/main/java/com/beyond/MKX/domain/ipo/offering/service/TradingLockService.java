package com.beyond.MKX.domain.ipo.offering.service;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingType;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradingLockService {
    private final StockRepository stockRepository;

    public void suspendTradingForOffering(IpoOffering offering) {
        if (offering.getOfferingType() == IpoOfferingType.FOLLOW_ON
                && offering.getRecordDate() != null) {
            UUID stockId = offering.getIpo().getStockId();
            if (stockId == null) {
                throw new IllegalArgumentException("해당 IPO에 연결된 stockId가 없습니다.");
            }

            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("종목이 존재하지 않습니다."));

            if (stock.getStatus() != Stock.Status.SUSPENDED) {
                stock.updateStatus(Stock.Status.SUSPENDED);
            }
        }
    }

    /**
     * 매일 새벽 6시 거래정지 해제 스케줄러
     * recordDate 이후 거래재개 처리
     */
    @Transactional
    @Scheduled(cron = "0 0 6 * * *")
    public void resumeTradingIfEligible() {
        LocalDate today = LocalDate.now();

        // 거래정지된 종목 중, recordDate 지난 공모 존재 시 해제
        List<Stock> suspendedStocks = stockRepository.findAllByStatus(Stock.Status.SUSPENDED);

        for (Stock stock : suspendedStocks) {
            // 조건 예: 특정 로직에서 recordDate + 1일 이후 해제
            // 이 예시에서는 단순히 하루 지난 것으로 간주
            stock.updateStatus(Stock.Status.LISTED);
        }

        stockRepository.saveAll(suspendedStocks);
    }
}
