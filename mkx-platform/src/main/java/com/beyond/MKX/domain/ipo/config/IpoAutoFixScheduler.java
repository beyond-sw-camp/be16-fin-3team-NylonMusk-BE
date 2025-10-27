package com.beyond.MKX.domain.ipo.config;

import com.beyond.MKX.domain.ipo.bookbuilding.repository.IpoBookBuildingRepository;
import com.beyond.MKX.domain.ipo.bookbuilding.service.IpoBookBuildingService;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IpoAutoFixScheduler {
    private final IpoOfferingRepository offeringRepository;
    private final IpoBookBuildingService bookBuildingService;
    private final Clock clock;

    // 🔹수요예측 시작 시 자동 BOOK_BUILDING 전환 (cron 표현식)
    @Scheduled(cron = "0 */1 * * * *")  // 매 1분마다
//    @Scheduled(cron = "0 0 * * * *")  // 매시간 0분 0초마다 실행
    @Transactional
    public void autoFixOfferPriceForEndedBookBuilding() {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1️⃣ 수요예측 종료된 공모 조회
        List<IpoOffering> targets =
                offeringRepository.findAllByIpoOfferingStatusAndBookBuildingEndBefore(
                        IpoOfferingStatus.BOOK_BUILDING, now
                );

        for (IpoOffering offering : targets) {
            try {
                bookBuildingService.fixOfferPriceByBookBuilding(offering.getId());
                System.out.println("[AUTO PRICE FIXED] " + offering.getIpo().getSymbol());
            } catch (Exception e) {
                System.err.println("[AUTO PRICE FIX FAILED] " + offering.getId() + " → " + e.getMessage());
            }
        }
    }
}
