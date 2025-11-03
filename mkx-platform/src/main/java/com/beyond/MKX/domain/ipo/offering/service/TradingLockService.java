package com.beyond.MKX.domain.ipo.offering.service;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingType;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TradingLockService {
    private final StockRepository stockRepository;
    private final IpoOfferingRepository offeringRepository;

    /**
     *   N차 공모(FOLLOW_ON, RIGHTS_ISSUE) 시 거래 정지 처리
     *  - recordDate가 설정된 경우에만 거래 정지로 전환
     *  - Stock.status → SUSPENDED
     */
    @Transactional
    public void suspendTradingForOffering(IpoOffering offering) {
        if ((offering.getOfferingType() == IpoOfferingType.FOLLOW_ON
                || offering.getOfferingType() == IpoOfferingType.RIGHTS_ISSUE)
                && offering.getRecordDate() != null) {

            UUID stockId = offering.getIpo().getStockId();
            if (stockId == null) {
                throw new IllegalArgumentException("해당 IPO에 연결된 stockId가 없습니다.");
            }

            Stock stock = stockRepository.findById(stockId)
                    .orElseThrow(() -> new IllegalArgumentException("종목이 존재하지 않습니다."));

            // 이미 정지 상태가 아니라면 정지로 전환
            if (stock.getStatus() != Stock.Status.SUSPENDED) {
                stock.updateStatus(Stock.Status.SUSPENDED);
            }
        }
    }

    /**
     * 거래 정지 해제 스케줄러
     * - 1분마다 실행
     * - recordDate + 3분이 지난 종목은 거래 재개(LISTED) 처리
     */
    @Transactional
    @Scheduled(cron = "0 */1 * * * *") // 1분마다 실행
    public void resumeTradingIfEligible() {
        LocalDateTime now = LocalDateTime.now();

        // 거래 정지된 종목 전체 조회
        List<Stock> suspendedStocks = stockRepository.findAllByStatus(Stock.Status.SUSPENDED);

        for (Stock stock : suspendedStocks) {
            // recordDate가 있고, FOLLOW_ON 또는 RIGHTS_ISSUE인 최신 공모를 찾음
            IpoOffering latestOffering = offeringRepository.findLatestWithRecordDateByStockId(
                            stock.getId(),
                            List.of(IpoOfferingType.FOLLOW_ON, IpoOfferingType.RIGHTS_ISSUE)
                    )
                    .orElse(null);

            if (latestOffering == null || latestOffering.getRecordDate() == null) continue;

            // recordDate + 3분 경과 시 거래 재개
//            LocalDateTime unlockTime = latestOffering.getRecordDate().plusMinutes(3);
            // recordDate + 5분 경과 시 거래 재개
            LocalDateTime unlockTime = latestOffering.getRecordDate().plusMinutes(5);

            if (now.isAfter(unlockTime)) {
                stock.updateStatus(Stock.Status.LISTED);
            }
        }

        // 상태 변경 반영
        stockRepository.saveAll(suspendedStocks);
    }
}
