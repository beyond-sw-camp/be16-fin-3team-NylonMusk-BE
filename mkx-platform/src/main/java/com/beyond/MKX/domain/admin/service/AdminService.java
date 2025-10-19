package com.beyond.MKX.domain.admin.service;

import com.beyond.MKX.domain.account.brokerage.entity.BrokerageDepositAccount;
import com.beyond.MKX.domain.account.brokerage.repository.BrokerageDepositAccountRepository;
import com.beyond.MKX.domain.account.corporation.entity.CorporationAccount;
import com.beyond.MKX.domain.account.corporation.repository.CorporationAccountRepository;
import com.beyond.MKX.domain.account.exchange.entity.ExchangeAccount;
import com.beyond.MKX.domain.account.exchange.repository.ExchangeAccountRepository;
import com.beyond.MKX.domain.admin.dto.AdminResDto;
import com.beyond.MKX.domain.admin.entity.Admin;
import com.beyond.MKX.domain.admin.entity.Role;
import com.beyond.MKX.domain.admin.repository.AdminRepository;
import com.beyond.MKX.domain.stock.entity.Stock;
import com.beyond.MKX.domain.stock.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final CorporationAccountRepository corporationAccountRepository;
    private final BrokerageDepositAccountRepository brokerageDepositAccountRepository;
    private final ExchangeAccountRepository exchangeAccountRepository;
    private final StockRepository stockRepository;

    /**
     * 관리자 ID로 엔티티 조회
     *
     * 주어진 식별자에 해당하는 관리자 엔티티를 반환합니다.
     * 존재하지 않으면 예외를 발생시켜 전역 예외 처리로 404/400에 매핑됩니다.
     *
     * @param userId 관리자 UUID
     * @return Admin 엔티티
     */
    public Admin getMemberById(UUID userId) {
        return adminRepository.findById(userId).orElseThrow(() ->
                new EntityNotFoundException("관리자를 찾을 수 없습니다."));
    }

    /**
     * 관리자 프로필 + 소속 조직의 계좌 번호/상태를 포함한 DTO 구성
     *
     * - 법인 계정 관리자: 최신 생성된 법인계좌를 조회하여 계좌번호/상태를 설정
     * - 증권사 관리자: 예치금 계좌 조회. 상태는 account_list 기반, 없으면 APPROVED로 간주
     * - 거래소 관리자: 임의 1건의 거래소 계좌를 조회하여 계좌번호/상태를 설정
     *
     * @param userId 관리자 UUID
     * @return AdminResDto(기본 프로필 + 계좌 정보)
     */
    public AdminResDto getProfileWithAccount(UUID userId) {
        // 기본 프로필 조회 및 DTO 변환
        Admin admin = getMemberById(userId);
        AdminResDto dto = AdminResDto.from(admin);

        // 법인 관리자: 최신 법인 계좌 1건 기준으로 계좌번호/상태 설정
        if (admin.getRole() == Role.CORPORATION && admin.getCorporation() != null) {
            // 기업은 계좌 1개만 보유한다는 전제 → 단건 조회
            corporationAccountRepository.findByCorporationId(admin.getCorporation().getId())
                    .ifPresent(acc -> {
                        dto.setAccountNumber(acc.getAccountNumber());
                        dto.setAccountStatus(acc.getStatus().name());
                    });
        // 증권사 관리자: 예치금 계좌(상태 개념 없음) → account_list 기준 사용, 없으면 APPROVED
        } else if (admin.getRole() == Role.BROKERAGE && admin.getSecuritiesFirm() != null) {
            brokerageDepositAccountRepository.findByBrokerageId(admin.getSecuritiesFirm().getId())
                    .ifPresent(acc -> {
                        dto.setAccountNumber(acc.getAccountNumber());
                        // 예치금 계좌는 상태 개념 없음 → account_list 기준으로 APPROVED
                        if (acc.getAccountList() != null) {
                            dto.setAccountStatus(acc.getAccountList().getStatus().name());
                        } else {
                            dto.setAccountStatus("APPROVED");
                        }
                    });
        // 거래소 관리자: 보유 계좌 중 임의 1건을 사용
        } else if (admin.getRole() == Role.EXCHANGE) {
            // 거래소 운영 계좌는 시스템상 1개만 존재 (seeder에서 count>0이면 생성 스킵)
            exchangeAccountRepository.findFirstByOrderByCreatedAtAsc().ifPresent(acc -> {
                dto.setAccountNumber(acc.getAccountNumber());
                if (acc.getAccountList() != null) {
                    dto.setAccountStatus(acc.getAccountList().getStatus().name());
                } else {
                    dto.setAccountStatus("APPROVED");
                }
            });
        }

        return dto;
    }

    /** 확장 프로필: 조직/상장 종목 정보까지 포함 */
    public AdminResDto getMeWithOrgsAndStocks(UUID userId) {
        AdminResDto dto = getProfileWithAccount(userId);
        Admin admin = getMemberById(userId);

        if (admin.getRole() == Role.CORPORATION && admin.getCorporation() != null) {
            dto.setCorporation(AdminResDto.CorporationBrief.builder()
                    .id(admin.getCorporation().getId())
                    .name(admin.getCorporation().getNameKo())
                    .build());
            // 종목 목록
            java.util.List<Stock> all = stockRepository.findAll().stream()
                    .filter(s -> s.getCorporationId().equals(admin.getCorporation().getId()))
                    .toList();
            java.util.List<AdminResDto.StockBrief> stocks = all.stream().map(s -> AdminResDto.StockBrief.builder()
                    .id(s.getId()).nameKo(s.getNameKo()).ticker(s.getTicker()).status(s.getStatus().name()).build()).toList();
            dto.setStocks(stocks);
            dto.setListedStocks(stocks.stream().filter(sb -> "LISTED".equals(sb.getStatus())).toList());
        } else if (admin.getRole() == Role.BROKERAGE && admin.getSecuritiesFirm() != null) {
            dto.setSecuritiesFirm(AdminResDto.SecuritiesFirmBrief.builder()
                    .id(admin.getSecuritiesFirm().getId())
                    .name(admin.getSecuritiesFirm().getNameKo())
                    .build());
        }
        return dto;
    }
}
