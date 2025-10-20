package com.beyond.MKX.domain.assets.service;

import com.beyond.MKX.domain.assets.dto.StockUpdateDTO;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.entity.StockUpdateEvent;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import com.beyond.MKX.domain.assets.repository.StockUpdateEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockUpdateApplyService {

    private final StockHoldingRepository stockHoldingRepository;
    private final StockUpdateEventRepository eventRepository;

    @Transactional
    public void apply(StockUpdateDTO dto) {
        // 0) 이벤트 선점: 이미 처리된 키면 바로 종료
        try {
            eventRepository.save(StockUpdateEvent.builder()
                    .idempotencyKey(dto.getIdempotencyKey())
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException e) {
            // UK(idempotency_key) 충돌 → 이미 처리됨
            return;
        }

        // 1) upsert
        StockHolding h = stockHoldingRepository
                .findByMemberAccountIdAndTicker(dto.getMemberAccountId(), dto.getTicker())
                .orElseGet(() -> StockHolding.builder()
                        .memberAccountId(dto.getMemberAccountId())
                        .brokerageId(dto.getBrokerageId())   // NULL 금지
                        .ticker(dto.getTicker())
                        .totalQuantity(0L)
                        .availableQuantity(0L)
                        .totalPurchasePrice(0L)
                        .build());

        // 2) 기존 엔티티는 증가 메서드/세터로 갱신 (새 빌더로 다시 만들지 말 것)
        h.increaseTotalQuantity(dto.getQtyDelta());
        h.increaseAvaQuantity(dto.getQtyDelta());
        if (dto.getUnitPrice() != null) {
            h.incTotalPurchasePrice(dto.getQtyDelta(), dto.getUnitPrice());
        }

        try {
            stockHoldingRepository.save(h);
        } catch (DataIntegrityViolationException dup) {
            // 동시 생성 경쟁: 다시 한 번 조회 후 업데이트만 수행
            StockHolding again = stockHoldingRepository
                    .findByMemberAccountIdAndTicker(dto.getMemberAccountId(), dto.getTicker())
                    .orElseThrow(() -> dup);
            // again에 동일 갱신 적용 후 save
            again.increaseTotalQuantity(dto.getQtyDelta());
            again.increaseAvaQuantity(dto.getQtyDelta());
            if (dto.getUnitPrice()!=null) again.incTotalPurchasePrice(dto.getQtyDelta(), dto.getUnitPrice());
            stockHoldingRepository.save(again);
        }

        // 3) 여기서 따로 이벤트를 또 저장할 필요 없음
        // 위의 선점 insert가 이 트랜잭션에 포함되어 커밋됩니다.
    }
}

