package com.beyond.MKX.domain.ipo.config;

import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class IpoOfferingStateScheduler {

    private final IpoOfferingRepository offeringRepository;
    private final Clock clock;

    /**
     * 매 1분마다 IPO 공모 상태 자동 전환
     * - SCHEDULED → BOOK_BUILDING
     * - BOOK_BUILDING → OPEN
     * - OPEN → CLOSED
     */
    @Scheduled(cron = "*/10 * * * * *") // 1분마다 실행
    @Transactional
    public void autoUpdateOfferingStates() {
        LocalDateTime now = LocalDateTime.now(clock);

        // 1️⃣ BOOK_BUILDING 시작
        List<IpoOffering> toBookBuilding = offeringRepository
                .findAllByIpoOfferingStatusAndBookBuildingStartBefore(
                        IpoOfferingStatus.SCHEDULED, now
                );

        for (IpoOffering offering : toBookBuilding) {
            offering.setIpoOfferingStatus(IpoOfferingStatus.BOOK_BUILDING);
            log.info("[AUTO] SCHEDULED → BOOK_BUILDING | symbol={}, id={}",
                    offering.getIpo().getSymbol(), offering.getId());
        }

        // 2️⃣ OPEN 시작
        List<IpoOffering> toOpen = offeringRepository
                .findAllByIpoOfferingStatusAndSubscriptionStartBefore(
                        IpoOfferingStatus.PRICE_FIXED, now
                );

        for (IpoOffering offering : toOpen) {
            offering.setIpoOfferingStatus(IpoOfferingStatus.OPEN);
            log.info("[AUTO] BOOK_BUILDING → OPEN | symbol={}, id={}",
                    offering.getIpo().getSymbol(), offering.getId());
        }

        // 3️⃣ CLOSED 종료
        List<IpoOffering> toClose = offeringRepository
                .findAllByIpoOfferingStatusAndSubscriptionEndBefore(
                        IpoOfferingStatus.OPEN, now
                );

        for (IpoOffering offering : toClose) {
            offering.setIpoOfferingStatus(IpoOfferingStatus.CLOSED);
            log.info("[AUTO] OPEN → CLOSED | symbol={}, id={}",
                    offering.getIpo().getSymbol(), offering.getId());
        }
    }
}
