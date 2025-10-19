package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisclosureQueryService {

    private final DisclosureRepository disclosureRepository;
    private final StockRepository stockRepository;

    public Page<DisclosureResDto> listApproved(DisclosureType type, String ticker, String title, Pageable pageable) {
        UUID stockId = null;
        if (ticker != null && !ticker.isBlank()) {
            Optional<Stock> stockOpt = stockRepository.findByTicker(ticker);
            if (stockOpt.isEmpty()) {
                return Page.empty(pageable);
            }
            stockId = stockOpt.get().getId();
        }
        return disclosureRepository.searchApproved(DisclosureStatus.APPROVED, type, stockId, title, pageable)
                .map(DisclosureMapper::toRes);
    }
}
