package com.beyond.MKX.domain.ipo.bookbuilding.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.corporation.entity.Corporation;
import com.beyond.MKX.domain.corporation.repository.CorporationRepository;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingAvailableResDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingCreateDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingIssuerViewDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingResDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.entity.IpoBookBuilding;
import com.beyond.MKX.domain.ipo.bookbuilding.entity.ParticipantType;
import com.beyond.MKX.domain.ipo.bookbuilding.repository.IpoBookBuildingRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.CoderResult;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IpoBookBuildingService {
    private final IpoBookBuildingRepository bookBuildingRepository;
    private final IpoOfferingRepository offeringRepository;
    private final AdminRepository adminRepository;
    private final CorporationRepository corporationRepository;

    @Transactional
    public IpoBookBuildingResDTO create(UUID offeringId, CustomAdminPrincipal principal, IpoBookBuildingCreateDTO createDTO) {
        Admin admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자 없음"));

        Corporation corporation = admin.getCorporation();
        if (corporation == null) throw new IllegalArgumentException("기업 소속 관리자가 아닙니다.");

        // 1) 무조건 path variable 기반으로 조회
        IpoOffering ipoOffering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음"));

        // 2) participant를 서버에서 강제 세팅
        createDTO.setParticipantId(corporation.getId());
        createDTO.setParticipantType(ParticipantType.CORPORATION);

        // 3) 상태 가드
        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.BOOK_BUILDING)
            throw new IllegalArgumentException("BOOK_BUILDING 상태에서만 수요예측 등록이 가능합니다.");

        // 4) 발행사 자기참여 금지 (Corporation Id로 비교)
        UUID issuerCorpId = ipoOffering.getIpo().getCorporation().getId();
        if (createDTO.getParticipantType() == ParticipantType.CORPORATION
                && createDTO.getParticipantId().equals(issuerCorpId)) {
            throw new IllegalArgumentException("발행사는 자기 공모의 수요예측에 참여할 수 없습니다.");
        }

        // 5) 수량 가드
        if (createDTO.getBidQuantity() <= 0) throw new IllegalArgumentException("희망수량은 0보다 커야 합니다.");
        if (createDTO.getBidQuantity() > ipoOffering.getOfferQuantity())
            throw new IllegalArgumentException("희망수량은 공모물량을 초과할 수 없습니다.");

        // 6) 중복 참여 가드 (path variable 사용)
        boolean alreadyParticipated = bookBuildingRepository
                .existsByIpoOffering_IdAndParticipantId(offeringId, createDTO.getParticipantId());
        if (alreadyParticipated) throw new IllegalArgumentException("이미 해당 공모에 수요예측 참여하셨습니다.");

        // 7) 희망가 밴드 가드
        Long bidPrice = createDTO.getBidPrice();
        if (bidPrice != null) {
            long min = ipoOffering.getPriceBandMin();
            long max = ipoOffering.getPriceBandMax();
            if (bidPrice < min || bidPrice > max)
                throw new IllegalArgumentException(String.format("희망가격은 공모가 밴드(%d~%d)를 벗어날 수 없습니다.", min, max));
        }

        // 8) 저장
        IpoBookBuilding entity = IpoBookBuilding.builder()
                .ipoOffering(ipoOffering)
                .participantType(createDTO.getParticipantType())
                .participantId(createDTO.getParticipantId())
                .bidPrice(createDTO.getBidPrice())
                .bidQuantity(createDTO.getBidQuantity())
                .acceptAllPrices(createDTO.getBidPrice() == null)
                .alreadyParticipated(true)
                .build();

        return IpoBookBuildingResDTO.from(bookBuildingRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<IpoBookBuildingResDTO> findAllByOfferingId(UUID offeringId, UUID participantId) {
        List<IpoBookBuilding> bookBuildings = bookBuildingRepository
                .findAllByIpoOffering_IdAndParticipantId(offeringId, participantId);
        if (bookBuildings.isEmpty()) {
            throw new IllegalArgumentException("해당 공모에 대한 수요예측 내역이 없습니다.");
        }
        return bookBuildings.stream()
                .map(IpoBookBuildingResDTO::from)
                .toList();
    }

    @Transactional
    public IpoOffering fixOfferPriceByBookBuilding(UUID offeringId) {

        /** 0️⃣ 사전 검증 — 상태 및 데이터 확인 */
        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음."));
        if (offering.getIpoOfferingStatus() == IpoOfferingStatus.PRICE_FIXED) {
            throw new IllegalStateException("이미 확정된 공모입니다.");
        }
        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.BOOK_BUILDING) {
            throw new IllegalArgumentException("BOOK_BUILDING 상태에서만 확정 가능합니다.");
        }

        List<IpoBookBuilding> bookBuildings = bookBuildingRepository.findAllByIpoOffering_Id(offeringId);
        if (bookBuildings.isEmpty()) {
            throw new IllegalArgumentException("수요 예측 데이터가 없습니다.");
        }

        long offerQuantity = offering.getOfferQuantity();
        long face = offering.getIpo().getFaceValue();
        long min = offering.getPriceBandMin();
        long max = offering.getPriceBandMax();

        /** 1️⃣ 가격대별 수요량 초기화 */
        Map<Long, Long> demandMap = new TreeMap<>();
        for (long price = min; price <= max; price += 100) {
            demandMap.put(price, 0L);
        }

        /** 2️⃣ 참여자별 응찰 가격·수량 반영 */
        for (IpoBookBuilding bb : bookBuildings) {
            long bid = (bb.getBidPrice() == null)
                    ? min   // 가격무관 응찰은 밴드 하단으로 처리
                    : Math.max(min, Math.min(max, bb.getBidPrice())); // 밴드 내로 보정
            demandMap.put(bid, demandMap.getOrDefault(bid, 0L) + bb.getBidQuantity());
        }

        /** 3️⃣ 전체 수요 및 경쟁률 계산 */
        long cumulative = bookBuildings.stream().mapToLong(IpoBookBuilding::getBidQuantity).sum();
        double ratio = (double) cumulative / offerQuantity;

        /** 4️⃣ 가중평균 희망가격 계산 (시장 기대치 반영) */
        long totalDemand = 0L;
        long weightedSum = 0L;
        for (IpoBookBuilding b : bookBuildings) {
            long bid = (b.getBidPrice() == null)
                    ? min
                    : Math.max(min, Math.min(max, b.getBidPrice()));
            weightedSum += bid * b.getBidQuantity();
            totalDemand += b.getBidQuantity();
        }
        long weightedAvgPrice = (totalDemand > 0) ? (weightedSum / totalDemand) : min;

        /** 5️⃣ 확정공모가 산정 로직 */
        long fixedPrice;
        final double X1 = 1.0;  // 경쟁률 하한 (미달)
        final double X2 = 2.0;  // 경쟁률 상한 (과열)
        final double X3 = 10.0; // 초과수요 기준
        final double alpha = 0.5; // 미달구간 희망가 반영비율

        if (ratio < X1) {
            // 미달구간 — 밴드 하단가 + 희망가 평균 50% 반영
            fixedPrice = Math.round(min + alpha * (weightedAvgPrice - min));
        } else if (ratio < X2) {
            // 적정수요구간 — 가중평균가와 상단가 혼합
            double normalized = (ratio - X1) / (X2 - X1); // 1.0→0, 2.0→1
            double avgWeight = 1.0 - 0.8 * normalized;
            double maxWeight = 0.8 * normalized;
            double finalPrice = weightedAvgPrice * avgWeight + max * maxWeight;
            fixedPrice = Math.round(finalPrice);
        } else if (ratio < X3) {
            // 과수요구간 — 상단가와 상위10% 희망가 평균 중 큰 값
            long topPrice = calculateTopPercentilePrice(bookBuildings, 0.10);
            fixedPrice = Math.max(max, topPrice);
        } else {
            // 초과수요구간 — 밴드 초과 10% 허용
            long overPrice = max + Math.round((max - min) * 0.1);
            fixedPrice = overPrice;
        }

        /** 6️⃣ 밴드 내 클램프 + 10원 단위 반올림 */
        if (ratio < X3) {
            fixedPrice = Math.max(min, Math.min(max, fixedPrice));
        }
        fixedPrice = Math.round(fixedPrice / 10.0) * 10; // 💰 10원 단위로 조정

        /** 7️⃣ 결과 저장 */
        offering.setCompetitionRatio(BigDecimal.valueOf(ratio).setScale(2, RoundingMode.HALF_UP));
        offering.fixOfferPrice(fixedPrice, min, max, face);

        return offeringRepository.save(offering);
    }
    /** 상위 N% 가격 평균 계산 헬퍼 (optional) */
    private long calculateTopPercentilePrice(List<IpoBookBuilding> list, double percentile) {
        if (list.isEmpty()) return 0L;
        int cutoff = (int) Math.ceil(list.size() * (1 - percentile));
        List<Long> sorted = list.stream()
                .map(b -> b.getBidPrice() == null ? 0L : b.getBidPrice())
                .sorted()
                .toList();
        return Math.round(sorted.subList(cutoff, sorted.size()).stream()
                .mapToLong(Long::longValue)
                .average().orElse(0.0));
    }

    /**
     * 🔹 수요예측 가능한 공모 목록 조회
     */
    @Transactional(readOnly = true)
    public List<IpoBookBuildingAvailableResDTO> findAllScheduledOfferings(UUID participantId) {
        List<IpoOffering> scheduledOfferings =
                offeringRepository.findAllByIpoOfferingStatus(IpoOfferingStatus.BOOK_BUILDING);

        if (scheduledOfferings.isEmpty()) {
            throw new IllegalArgumentException("현재 수요예측 가능한(SCHEDULED) 공모가 없습니다.");
        }

        return scheduledOfferings.stream()
                .filter(offering ->
                        !bookBuildingRepository.existsByIpoOffering_IdAndParticipantId(offering.getId(), participantId)) // 🔸 이미 참여한 공모 제외
                .map(offering -> IpoBookBuildingAvailableResDTO.builder()
                        .ipoOfferingId(offering.getId())
                        .ipoId(offering.getIpo().getId())
                        .ipoSymbol(offering.getIpo().getSymbol())
                        .corporationName(offering.getIpo().getCorporation().getNameKo())
                        .offerQuantity(offering.getOfferQuantity())
                        .lotSize(offering.getLotSize())
                        .priceBandMin(offering.getPriceBandMin())
                        .priceBandMax(offering.getPriceBandMax())
                        .depositRate(offering.getDepositRate())
                        .status(offering.getIpoOfferingStatus().toString())
                        .alreadyParticipated(false) // 🔸 항상 false
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<IpoBookBuildingIssuerViewDTO> findAllForIssuer(UUID offeringId, UUID issuerCorpId) {
        IpoOffering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음"));

        // 자기 공모만 조회 가능하도록 가드
        if (!offering.getIpo().getCorporation().getId().equals(issuerCorpId)) {
            throw new IllegalArgumentException("본인 발행 공모만 조회할 수 있습니다.");
        }

        List<IpoBookBuilding> bookBuildings = bookBuildingRepository.findAllByIpoOffering_Id(offeringId);
        if (bookBuildings.isEmpty()) {
            throw new IllegalArgumentException("해당 공모에 대한 수요예측 참여 데이터가 없습니다.");
        }

        return bookBuildings.stream()
                .map(b -> {
                    // 참여 기업 이름 조회
                    String participantName = corporationRepository.findById(b.getParticipantId())
                            .map(Corporation::getNameKo)
                            .orElse("알 수 없음");
                    return IpoBookBuildingIssuerViewDTO.from(b, participantName);
                })
                .collect(Collectors.toList());
    }
}
