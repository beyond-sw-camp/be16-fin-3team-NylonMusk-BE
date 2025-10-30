package com.beyond.MKX.domain.execution.service;

import com.beyond.MKX.domain.assets.dto.StockInfoResDTO;
import com.beyond.MKX.domain.execution.dto.LedgerResponseDTO;
import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import com.beyond.MKX.domain.execution.repository.LedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LedgerService {
    private final LedgerRepository ledgerRepository;
    private final LedgerStockFeign stockFeign;

    /**
     * 특정 계좌의 거래내역을 조회합니다.
     * 
     * @param memberAccountId 계좌 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param type 거래 유형 필터 (null이면 전체)
     * @return 거래내역 페이지
     */
    public Page<LedgerResponseDTO> getMyLedgers(UUID memberAccountId, int page, int size, String type) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Ledger> ledgers;

        // 거래 유형 필터링
        if (type != null && !type.isEmpty() && !type.equals("ALL")) {
            try {
                TransactionType transactionType = TransactionType.valueOf(type.toUpperCase());
                ledgers = ledgerRepository.findByCreditAccountIdOrDebitAccountIdAndTransactionTypeOrderByCreatedAtDesc(
                    memberAccountId, memberAccountId, transactionType, pageable);
            } catch (IllegalArgumentException e) {
                // 잘못된 타입인 경우 전체 조회
                ledgers = ledgerRepository.findByCreditAccountIdOrDebitAccountIdOrderByCreatedAtDesc(
                    memberAccountId, memberAccountId, pageable);
            }
        } else {
            // 전체 조회
            ledgers = ledgerRepository.findByCreditAccountIdOrDebitAccountIdOrderByCreatedAtDesc(
                memberAccountId, memberAccountId, pageable);
        }

        // Ledger를 LedgerResponseDTO로 변환 (종목명 조회 포함)
        return ledgers.map(ledger -> {
            try {
                // Feign으로 종목명 조회
                StockInfoResDTO stockInfo = stockFeign.getStockByTicker(ledger.getTicker());
                String stockName = stockInfo != null ? stockInfo.getNameKo() : "종목명없음";
                return LedgerResponseDTO.from(ledger, stockName);
            } catch (Exception e) {
                // Feign 호출 실패 시 종목명 없이 반환
                return LedgerResponseDTO.from(ledger, "종목명없음");
            }
        });
    }
}

