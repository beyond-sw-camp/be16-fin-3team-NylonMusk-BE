package com.beyond.MKX.domain.ipo.ipo.service;

import com.beyond.MKX.domain.ipo.ipo.dto.IpoListResDTO;
import com.beyond.MKX.domain.ipo.ipo.dto.IpoRequestItemDTO;
import com.beyond.MKX.domain.ipo.ipo.entity.Ipo;
import com.beyond.MKX.domain.ipo.ipo.entity.IpoStatus;
import com.beyond.MKX.domain.ipo.ipo.repository.IpoRepository;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOffering;
import com.beyond.MKX.domain.ipo.offering.entity.IpoOfferingStatus;
import com.beyond.MKX.domain.ipo.offering.repository.IpoOfferingRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IpoApprovalService {
    private final IpoRepository ipoRepository;
    private final IpoOfferingRepository offeringRepository;
    private final StockRepository stockRepository;

    @Transactional(readOnly = true)
    public Page<IpoRequestItemDTO> listRequests(
            Set<IpoStatus> statuses, String q,
            LocalDateTime from, LocalDateTime to, Pageable pageable) {

        // 기본 상태: 요청/심사/승인(상장 전 단계)
        if (statuses == null || statuses.isEmpty()) {
            statuses = Set.of(IpoStatus.REQUESTED, IpoStatus.UNDER_REVIEW, IpoStatus.APPROVED);
        }
        Page<Ipo> page = ipoRepository.searchRequests(statuses, q, from, to, pageable);
        return page.map(IpoRequestItemDTO::of);
    }
}
