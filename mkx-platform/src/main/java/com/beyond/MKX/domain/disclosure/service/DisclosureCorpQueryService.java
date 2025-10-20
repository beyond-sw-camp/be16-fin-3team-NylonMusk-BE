package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.common.auth.security.CustomAdminPrincipal;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.disclosure.dto.DisclosureResDto;
import com.beyond.MKX.domain.disclosure.dto.DisclosureTreeResDto;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.entity.DisclosureStatus;
import com.beyond.MKX.domain.disclosure.entity.DisclosureType;
import com.beyond.MKX.domain.disclosure.mapper.DisclosureMapper;
import com.beyond.MKX.domain.disclosure.repository.DisclosureRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisclosureCorpQueryService {

    private final DisclosureRepository disclosureRepository;
    private final AdminRepository adminRepository;
    private final StockRepository stockRepository;
    private final DisclosureAdminQueryService adminQueryService;

    public Page<DisclosureResDto> listMine(
            DisclosureStatus status,
            DisclosureType type,
            String title,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    ) {
        UUID corpId = currentCorporationId();
        LocalDateTime from = (fromDate != null) ? fromDate.atStartOfDay() : null;
        LocalDateTime toExclusive = (toDate != null) ? toDate.plusDays(1).atStartOfDay() : null;
        return disclosureRepository.searchByCorporation(corpId, status, type, title, from, toExclusive, pageable)
                .map(DisclosureMapper::toRes);
    }

    /**
     * 기업용: 본공시 번호 기준 관련 트리 조회(본공시 체인 + 추가공시 체인)
     * - 접근 제어: 해당 baseNo 체인이 내 기업 종목의 공시인지 검증
     */
    public DisclosureTreeResDto getRelatedTreeMineByBaseNo(String baseNo) {
        UUID corpId = currentCorporationId();
        List<Disclosure> chain = disclosureRepository.findRevisionsByDisplayNo(baseNo);
        if (chain.isEmpty()) {
            throw new IllegalArgumentException("해당 공시 번호의 이력이 없습니다.");
        }
        // 체인 중 아무 항목이나 소유 기업 확인(동일 종목 체인)
        UUID stockId = chain.get(0).getStockId();
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다."));
        if (!corpId.equals(stock.getCorporationId())) {
            throw new IllegalArgumentException("내 기업의 공시가 아닙니다.");
        }
        return adminQueryService.getRelatedTreeByBaseNo(baseNo);
    }

    private UUID currentCorporationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomAdminPrincipal principal)) {
            throw new IllegalArgumentException("인증되지 않은 요청입니다.");
        }
        Admin admin = adminRepository.findById(principal.id())
                .orElseThrow(() -> new IllegalArgumentException("관리자를 찾을 수 없습니다."));
        if (admin.getCorporation() == null) {
            throw new IllegalArgumentException("기업 관리자 계정이 아닙니다.");
        }
        return admin.getCorporation().getId();
    }
}
