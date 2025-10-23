package com.beyond.MKX.domain.ipo.bookbuilding.service;

import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingCreateDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.dto.IpoBookBuildingResDTO;
import com.beyond.MKX.domain.ipo.bookbuilding.entity.IpoBookBuilding;
import com.beyond.MKX.domain.ipo.bookbuilding.repository.IpoBookBuildingRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IpoBookBuildingService {
    private final IpoBookBuildingRepository bookBuildingRepository;
    private final IpoOfferingRepository offeringRepository;

    @Transactional
    public IpoBookBuildingResDTO create(IpoBookBuildingCreateDTO createDTO) {
        IpoOffering ipoOffering = offeringRepository.findByIdForUpdate(createDTO.getIpoOfferingId())
                .orElseThrow(() -> new IllegalArgumentException("공모 없음"));

        if (ipoOffering.getIpoOfferingStatus() != IpoOfferingStatus.SCHEDULED) {
            throw new IllegalArgumentException("SCHEDULED 상태에서만 수요예측 등록이 가능합니다.");
        }

        IpoBookBuilding ipoBookBuilding = IpoBookBuilding.builder()
                .ipoOffering(ipoOffering)
                .participantType(createDTO.getParticipantType())
                .participantId(createDTO.getParticipantId())
                .bidPrice(createDTO.getBidPrice())
                .bidQuantity(createDTO.getBidQuantity())
                .acceptAllPrices(createDTO.getBidPrice() == null)
                .build();

        IpoBookBuilding saved = bookBuildingRepository.save(ipoBookBuilding);
        return IpoBookBuildingResDTO.from(saved);
    }

    @Transactional
    public List<IpoBookBuildingResDTO> findAllByOfferingId(UUID offeringId) {
        List<IpoBookBuilding> bookBuildings = bookBuildingRepository.findAllByIpoOffering_Id(offeringId);
        if (bookBuildings.isEmpty()) {
            throw new IllegalArgumentException("해당 공모에 대한 수요예측 내역이 없습니다.");
        }
        return bookBuildings.stream().map(IpoBookBuildingResDTO::from).toList();
    }

    @Transactional
    public IpoOffering finalizeOfferPriceByBookBuilding(UUID offeringId) {
        IpoOffering offering = offeringRepository.findByIdForUpdate(offeringId)
                .orElseThrow(() -> new IllegalArgumentException("공모 없음."));
        if (offering.getIpoOfferingStatus() != IpoOfferingStatus.SCHEDULED) {
            throw new IllegalArgumentException("SCHEDULED 상태에서만 수요예측 결과 ' 확정 ' 이 가능합니다.");
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
                for (long price : demandMap.keySet()) {
                    demandMap.put(price, demandMap.get(price) + bookBuilding.getBidQuantity());
                }
            }  else {
                long bidPrice = Math.max(min, Math.min(max, bookBuilding.getBidPrice())); // 범위 보정
                demandMap.put(bidPrice, demandMap.getOrDefault(bidPrice, 0L) + bookBuilding.getBidQuantity());
            }
        }

        /** 2) 누적 수요 기준, 경쟁률 계산 */
        long cumulative = 0;
        double ratio = 0.0;

        for (Map.Entry<Long, Long> entry : demandMap.entrySet()) {
            cumulative += entry.getValue();
            ratio = (double) cumulative / offerQuantity;
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

}
