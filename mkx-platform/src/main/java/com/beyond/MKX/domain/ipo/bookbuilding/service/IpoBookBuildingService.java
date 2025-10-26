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
        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음."));
        if (offering.getIpoOfferingStatus() == IpoOfferingStatus.PRICE_FIXED) {
            throw new IllegalStateException("이미 확정된 공모입니다.");
        }

        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.BOOK_BUILDING) {
            throw new IllegalArgumentException("BOOK_BUILDING 상태에서만 수요예측 결과 ' 확정 ' 이 가능합니다.");
        }

        List<IpoBookBuilding> bookBuildings = bookBuildingRepository.findAllByIpoOffering_Id(offeringId);
        if (bookBuildings.isEmpty()) {
            throw new IllegalArgumentException("수요 예측 데이터가 없습니다.");
        }

        long offerQuantity = offering.getOfferQuantity();
        long face = offering.getIpo().getFaceValue();
        long min = offering.getPriceBandMin();
        long max = offering.getPriceBandMax();

        /** 1) 가격대별 수요량 집계 */
        Map<Long, Long> demandMap = new TreeMap<>();
        for (long price = min; price <= max; price += 100) {
            demandMap.put(price, 0L);
        }

        for (IpoBookBuilding bookBuilding : bookBuildings) {
//            가격 무관 참여의 경우 ( acceptAllPrices = true || 희망가 미기재 (bidPrice == null) )
            if (Boolean.TRUE.equals(bookBuilding.getAcceptAllPrices()) || bookBuilding.getBidPrice() == null) {
                // 🔹 가격 무관 응찰 → 밴드 하단(min)에 1회만 반영 (한국거래소·금융투자협회 기관청약시스템 로직과 동일)
                demandMap.put(min, demandMap.get(min) + bookBuilding.getBidQuantity());
            } else {
                long bidPrice = Math.max(min, Math.min(max, bookBuilding.getBidPrice())); // 범위 보정
                demandMap.put(bidPrice, demandMap.getOrDefault(bidPrice, 0L) + bookBuilding.getBidQuantity());
            }
        }

        /** 2) 누적 수요 기준, 경쟁률 계산 */
        long cumulative = 0;
        double ratio = 0.0;
        long equilibriumPrice = min; // 추후 균형가격(시장 수요곡선의 교차지점)을 계산하여 확정공모가

        for (Map.Entry<Long, Long> entry : demandMap.entrySet()) {
            cumulative += entry.getValue();
            ratio = (double) cumulative / offerQuantity;

            if (cumulative >= offerQuantity) {
                equilibriumPrice = entry.getKey();
                break; // 최초로 수요가 공급량을 초과한 시점
            }
        }

        /** 3) 경쟁률 기록 + 확정 공모가 반영 */
        long fixedPrice;

        if (ratio < 1.0) {
            // 미달 : 밴드 하단(priceBandMin)
            fixedPrice = min;
        } else if (ratio >= 3.0) {
            // 과열 : 밴드 상단(priceBandMax)
            fixedPrice = max;
        } else {
            // 1.0 <= ratio < 3.0 : 밴드 내 선형 증가
            double normalized = (ratio - 1.0) / (3.0 - 1.0); // 0 ~ 1 범위로 정규화
            double interpolated = min + (max - min) * normalized;
            fixedPrice = BigDecimal.valueOf(interpolated)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();
        }
        /** 4) 경쟁률 기록 + 확정 공모가 반영 */
        offering.setCompetitionRatio(BigDecimal.valueOf(ratio).setScale(2, RoundingMode.HALF_UP));
        offering.fixOfferPrice(fixedPrice, min, max, face);
        return offeringRepository.save(offering);
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
                .filter(offering -> !bookBuildingRepository
                        .existsByIpoOffering_IdAndParticipantId(offering.getId(), participantId)) // 🔸 이미 참여한 공모 제외
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
