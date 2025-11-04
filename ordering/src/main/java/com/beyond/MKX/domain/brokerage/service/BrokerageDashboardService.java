package com.beyond.MKX.domain.brokerage.service;

import com.beyond.MKX.domain.account.member.client.MemberInternalClient;
import com.beyond.MKX.domain.assets.dto.StockInfoResDTO;
import com.beyond.MKX.domain.assets.entity.AccountStatus;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import com.beyond.MKX.domain.assets.service.StockFeign;
import com.beyond.MKX.domain.brokerage.dto.BrokerageStatsDTO;
import com.beyond.MKX.domain.brokerage.dto.LedgerDTO;
import com.beyond.MKX.domain.brokerage.dto.OrderLogDTO;
import com.beyond.MKX.domain.brokerage.dto.PopularStockDTO;
import com.beyond.MKX.domain.brokerage.dto.RecentActivityDTO;
import com.beyond.MKX.domain.execution.entity.Ledger;
import com.beyond.MKX.domain.execution.entity.TransactionType;
import com.beyond.MKX.domain.execution.repository.LedgerRepository;
import com.beyond.MKX.domain.order.entity.OrderLog;
import com.beyond.MKX.domain.order.entity.OrderStatus;
import com.beyond.MKX.domain.order.entity.Side;
import com.beyond.MKX.domain.order.repository.OrderLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BrokerageDashboardService {

    private final OrderLogRepository orderLogRepository;
    private final LedgerRepository ledgerRepository;
    private final MemberAccountRepository memberAccountRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final StockFeign stockFeign;
    private final MemberInternalClient memberInternalClient;

    /**
     * 증권사별 최근 활동 목록 조회 (order_log + ledger 통합)
     * - 최근 생성일시 기준으로 정렬하여 반환
     */
    @Transactional(readOnly = true)
    public List<RecentActivityDTO> getRecentActivities(UUID brokerageId, int limit) {
        Pageable pageable = PageRequest.of(0, limit * 2, Sort.by(Sort.Direction.DESC, "createdAt"));

        // 1. order_log 조회
        Page<OrderLog> orderLogs = orderLogRepository.findByBrokerageIdOrderByCreatedAtDesc(brokerageId, pageable);
        List<RecentActivityDTO> activities = new ArrayList<>();

        for (OrderLog orderLog : orderLogs.getContent()) {
            try {
                MemberAccount account = orderLog.getAccount();
                String accountNumber = account != null ? account.getNumber() : "N/A";
                String memberName = getMemberName(account);

                String stockName = getStockName(orderLog.getTicker());

                RecentActivityDTO activity = RecentActivityDTO.builder()
                    .id(orderLog.getId())
                    .type("ORDER")
                    .activityType(orderLog.getSide().name())
                    .activityTypeKorean(getOrderActivityTypeKorean(orderLog.getSide(), orderLog.getStatus()))
                    .ticker(orderLog.getTicker())
                    .stockName(stockName)
                    .quantity(orderLog.getQuantity())
                    .amount(orderLog.getPrice() != null ? orderLog.getPrice() * orderLog.getQuantity() : 0L)
                    .accountNumber(accountNumber)
                    .memberName(memberName)
                    .createdAt(orderLog.getCreatedAt())
                    .build();
                activities.add(activity);
            } catch (Exception e) {
                log.warn("Failed to process order log {}: {}", orderLog.getId(), e.getMessage());
            }
        }

        // 2. ledger 조회
        Page<Ledger> ledgers = ledgerRepository.findByBrokerageIdOrderByCreatedAtDesc(brokerageId, pageable);
        for (Ledger ledger : ledgers.getContent()) {
            try {
                // ledger의 creditAccountId 또는 debitAccountId가 증권사 계좌가 아닌 회원 계좌인 경우 조회
                UUID memberAccountId = null;
                if (ledger.getCreditAccountId() != null && !ledger.getCreditAccountId().equals(brokerageId)) {
                    memberAccountId = ledger.getCreditAccountId();
                } else if (ledger.getDebitAccountId() != null && !ledger.getDebitAccountId().equals(brokerageId)) {
                    memberAccountId = ledger.getDebitAccountId();
                }
                
                final String[] accountNumber = {"N/A"};
                final String[] memberName = {"N/A"};
                
                if (memberAccountId != null) {
                    memberAccountRepository.findById(memberAccountId)
                        .ifPresent(account -> {
                            accountNumber[0] = account.getNumber();
                            memberName[0] = getMemberName(account);
                        });
                }

                String stockName = ledger.getTicker() != null ? getStockName(ledger.getTicker()) : "N/A";

                RecentActivityDTO activity = RecentActivityDTO.builder()
                    .id(ledger.getId())
                    .type("LEDGER")
                    .activityType(ledger.getTransactionType() != null ? ledger.getTransactionType().name() : "UNKNOWN")
                    .activityTypeKorean(getTransactionTypeKorean(ledger.getTransactionType()))
                    .ticker(ledger.getTicker())
                    .stockName(stockName)
                    .quantity(ledger.getQtyChange())
                    .amount(ledger.getCredit() != null ? ledger.getCredit() : ledger.getDebit())
                    .accountNumber(accountNumber[0])
                    .memberName(memberName[0])
                    .createdAt(ledger.getCreatedAt())
                    .build();
                activities.add(activity);
            } catch (Exception e) {
                log.warn("Failed to process ledger {}: {}", ledger.getId(), e.getMessage());
            }
        }

        // 3. 생성일시 기준으로 정렬하고 limit만큼 반환
        return activities.stream()
            .sorted(Comparator.comparing(RecentActivityDTO::getCreatedAt).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 증권사별 주문 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<OrderLogDTO> getOrderLogs(UUID brokerageId, Pageable pageable) {
        Page<OrderLog> orderLogs = orderLogRepository.findByBrokerageIdOrderByCreatedAtDesc(brokerageId, pageable);

        return orderLogs.map(orderLog -> {
            try {
                MemberAccount account = orderLog.getAccount();
                String accountNumber = account != null ? account.getNumber() : "N/A";
                String memberName = getMemberName(account);
                String stockName = getStockName(orderLog.getTicker());

                return OrderLogDTO.builder()
                    .id(orderLog.getId())
                    .ticker(orderLog.getTicker())
                    .stockName(stockName)
                    .orderKind(orderLog.getOrderKind().name())
                    .side(orderLog.getSide().name())
                    .status(orderLog.getStatus().name())
                    .price(orderLog.getPrice())
                    .quantity(orderLog.getQuantity())
                    .remainQuantity(orderLog.getRemainQuantity())
                    .commission(orderLog.getCommission())
                    .transactionTax(orderLog.getTransactionTax())
                    .accountNumber(accountNumber)
                    .memberName(memberName)
                    .createdAt(orderLog.getCreatedAt())
                    .filledAt(orderLog.getFilledAt())
                    .canceledAt(orderLog.getCanceledAt())
                    .build();
            } catch (Exception e) {
                log.warn("Failed to convert order log {}: {}", orderLog.getId(), e.getMessage());
                return OrderLogDTO.builder()
                    .id(orderLog.getId())
                    .ticker(orderLog.getTicker())
                    .stockName("N/A")
                    .orderKind(orderLog.getOrderKind().name())
                    .side(orderLog.getSide().name())
                    .status(orderLog.getStatus().name())
                    .price(orderLog.getPrice())
                    .quantity(orderLog.getQuantity())
                    .remainQuantity(orderLog.getRemainQuantity())
                    .commission(orderLog.getCommission())
                    .transactionTax(orderLog.getTransactionTax())
                    .accountNumber("N/A")
                    .memberName("N/A")
                    .createdAt(orderLog.getCreatedAt())
                    .filledAt(orderLog.getFilledAt())
                    .canceledAt(orderLog.getCanceledAt())
                    .build();
            }
        });
    }

    /**
     * 증권사별 통계 조회 (총 고객 수, 활성 계좌 수, 일일 거래량, 월간 수익, 변화율)
     */
    @Transactional(readOnly = true)
    public BrokerageStatsDTO getBrokerageStats(UUID brokerageId) {
        // 1. 총 고객 수 및 활성 계좌 수 조회
        List<MemberAccount> allAccounts = memberAccountRepository.findAllByBrokerageId(brokerageId);
        long totalCustomers = allAccounts.stream()
            .map(MemberAccount::getMemberId)
            .distinct()
            .count();
        
        long activeAccounts = allAccounts.stream()
            .filter(acc -> acc.getStatus() == AccountStatus.ACTIVE)
            .count();
        
        // 3. 오늘 주문한 고객 수 (오늘 00:00 ~ 23:59:59 안에 주문을 넣은 회원 계좌 수)
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.atTime(LocalTime.MAX).plusSeconds(1); // 23:59:59까지 포함
        
        Long onlineCustomersCount = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
            brokerageId, todayStart, todayEnd);
        long onlineCustomers = onlineCustomersCount != null ? onlineCustomersCount : 0L;
        
        // 4. 일일 거래량 (오늘)
        LocalDateTime todayVolumeStart = today.atStartOfDay();
        LocalDateTime todayVolumeEnd = today.atTime(LocalTime.MAX);
        
        Long dailyVolume = ledgerRepository.getDailyVolumeByBrokerageId(brokerageId, todayVolumeStart, todayVolumeEnd);
        if (dailyVolume == null) {
            dailyVolume = 0L;
        }
        
        // 5. 전일 거래량 (어제)
        LocalDate yesterday = today.minusDays(1);
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime yesterdayEnd = yesterday.atTime(LocalTime.MAX);
        
        Long yesterdayVolume = ledgerRepository.getDailyVolumeByBrokerageId(brokerageId, yesterdayStart, yesterdayEnd);
        if (yesterdayVolume == null || yesterdayVolume == 0) {
            yesterdayVolume = 1L; // 0으로 나누기 방지
        }
        
        // 6. 거래량 변화율 계산
        double volumeChangePercent = 0.0;
        if (yesterdayVolume > 1L) {
            volumeChangePercent = ((double)(dailyVolume - yesterdayVolume) / yesterdayVolume) * 100.0;
        } else if (dailyVolume > 0) {
            volumeChangePercent = 100.0; // 전일 거래가 없었는데 오늘 거래가 있으면 100% 증가
        }
        
        // 5. 월간 수익 (최근 30일)
        // 매수(BUY)와 매도(SELL) 거래의 수수료를 합산하여 월간 수익 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);
        
        // 최근 30일 매수 수수료
        Long buyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.BUY, thirtyDaysAgo, now);
        if (buyCommission == null) {
            buyCommission = 0L;
        }
        
        // 최근 30일 매도 수수료
        Long sellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.SELL, thirtyDaysAgo, now);
        if (sellCommission == null) {
            sellCommission = 0L;
        }
        
        Long monthlyRevenue = buyCommission + sellCommission;
        
        // 6. 이전 30일 수익 (30일 전 ~ 60일 전)
        Long previousBuyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.BUY, sixtyDaysAgo, thirtyDaysAgo);
        if (previousBuyCommission == null) {
            previousBuyCommission = 0L;
        }
        
        Long previousSellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
            brokerageId, TransactionType.SELL, sixtyDaysAgo, thirtyDaysAgo);
        if (previousSellCommission == null) {
            previousSellCommission = 0L;
        }
        
        Long previousRevenue = previousBuyCommission + previousSellCommission;
        if (previousRevenue == 0) {
            previousRevenue = 1L; // 0으로 나누기 방지
        }
        
        // 7. 수익 변화율 계산
        double revenueChangePercent = 0.0;
        if (previousRevenue > 1L) {
            revenueChangePercent = ((double)(monthlyRevenue - previousRevenue) / previousRevenue) * 100.0;
        } else if (monthlyRevenue > 0) {
            revenueChangePercent = 100.0; // 이전 30일 수익이 없었는데 최근 30일 수익이 있으면 100% 증가
        }
        
        // 8. 7일 전 데이터와 비교하여 변화량 계산
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        
        // 7일 전 기준 총 고객 수 (7일 전 이전에 생성된 계좌만)
        long sevenDaysAgoTotalCustomers = allAccounts.stream()
            .filter(acc -> acc.getCreatedAt() != null && acc.getCreatedAt().isBefore(sevenDaysAgo))
            .map(MemberAccount::getMemberId)
            .distinct()
            .count();
        
        // 7일 전 기준 활성 계좌 수 (7일 전 이전에 생성되고 현재도 활성 상태인 계좌)
        long sevenDaysAgoActiveAccounts = allAccounts.stream()
            .filter(acc -> acc.getCreatedAt() != null && acc.getCreatedAt().isBefore(sevenDaysAgo))
            .filter(acc -> acc.getStatus() == AccountStatus.ACTIVE)
            .count();
        
        // 변화량 계산 (현재 - 7일 전)
        long customerChange = totalCustomers - sevenDaysAgoTotalCustomers;
        long accountChange = activeAccounts - sevenDaysAgoActiveAccounts;
        
        return BrokerageStatsDTO.builder()
            .totalCustomers(totalCustomers)
            .activeAccounts(activeAccounts)
            .onlineCustomers(onlineCustomers)
            .dailyVolume(dailyVolume)
            .monthlyRevenue(monthlyRevenue)
            .buyCommission(buyCommission)
            .sellCommission(sellCommission)
            .volumeChangePercent(volumeChangePercent)
            .revenueChangePercent(revenueChangePercent)
            .customerChange(customerChange)
            .accountChange(accountChange)
            .build();
    }

    /**
     * 증권사별 인기 종목 조회 (보유 고객 수 기준 TOP 10)
     */
    @Transactional(readOnly = true)
    public List<PopularStockDTO> getPopularStocks(UUID brokerageId, int limit) {
        // 1. 해당 증권사의 모든 stock_holding 조회
        List<StockHolding> holdings = stockHoldingRepository.findAllByBrokerageId(brokerageId);
        
        // 2. ticker별로 그룹화하여 집계
        Map<String, List<StockHolding>> holdingsByTicker = holdings.stream()
            .filter(h -> h.getTotalQuantity() > 0) // 보유 수량이 0보다 큰 것만
            .collect(Collectors.groupingBy(StockHolding::getTicker));
        
        // 3. 각 ticker별로 PopularStockDTO 생성
        List<PopularStockDTO> popularStocks = new ArrayList<>();
        for (Map.Entry<String, List<StockHolding>> entry : holdingsByTicker.entrySet()) {
            String ticker = entry.getKey();
            List<StockHolding> tickerHoldings = entry.getValue();
            
            // 보유 고객 수 = distinct memberAccountId 개수
            long customerCount = tickerHoldings.stream()
                .map(StockHolding::getMemberAccountId)
                .distinct()
                .count();
            
            // 총 보유량 = totalQuantity 합계
            long totalQuantity = tickerHoldings.stream()
                .mapToLong(StockHolding::getTotalQuantity)
                .sum();
            
            // 종목명 조회
            String stockName = getStockName(ticker);
            
            popularStocks.add(PopularStockDTO.builder()
                .ticker(ticker)
                .stockName(stockName)
                .customerCount(customerCount)
                .totalQuantity(totalQuantity)
                .build());
        }
        
        // 4. 보유 고객 수 기준으로 정렬하고 limit만큼 반환
        return popularStocks.stream()
            .sorted(Comparator.comparing(PopularStockDTO::getCustomerCount).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 증권사별 거래내역 조회
     */
    @Transactional(readOnly = true)
    public Page<LedgerDTO> getLedgers(UUID brokerageId, Pageable pageable) {
        Page<Ledger> ledgers = ledgerRepository.findByBrokerageIdOrderByCreatedAtDesc(brokerageId, pageable);

        return ledgers.map(ledger -> {
            try {
                UUID memberAccountId = null;
                if (ledger.getCreditAccountId() != null && !ledger.getCreditAccountId().equals(brokerageId)) {
                    memberAccountId = ledger.getCreditAccountId();
                } else if (ledger.getDebitAccountId() != null && !ledger.getDebitAccountId().equals(brokerageId)) {
                    memberAccountId = ledger.getDebitAccountId();
                }
                
                final String[] accountNumber = {"N/A"};
                final String[] memberName = {"N/A"};
                
                if (memberAccountId != null) {
                    memberAccountRepository.findById(memberAccountId)
                        .ifPresent(account -> {
                            accountNumber[0] = account.getNumber();
                            memberName[0] = getMemberName(account);
                        });
                }

                String stockName = ledger.getTicker() != null ? getStockName(ledger.getTicker()) : "N/A";

                return LedgerDTO.builder()
                    .id(ledger.getId())
                    .orderLogId(ledger.getOrderLogId())
                    .ticker(ledger.getTicker())
                    .stockName(stockName)
                    .transactionType(ledger.getTransactionType() != null ? ledger.getTransactionType().name() : "UNKNOWN")
                    .transactionTypeKorean(getTransactionTypeKorean(ledger.getTransactionType()))
                    .quantity(ledger.getQtyChange())
                    .price(ledger.getAmountChange())
                    .totalAmount(ledger.getCredit() != null ? ledger.getCredit() : ledger.getDebit())
                    .debit(ledger.getDebit())
                    .credit(ledger.getCredit())
                    .commission(ledger.getCommission())
                    .tax(ledger.getTax())
                    .accountNumber(accountNumber[0])
                    .memberName(memberName[0])
                    .counterpartyAccountNumber(ledger.getCounterpartyAccountNumber())
                    .counterpartyName(ledger.getCounterpartyName())
                    .createdAt(ledger.getCreatedAt())
                    .build();
            } catch (Exception e) {
                log.warn("Failed to convert ledger {}: {}", ledger.getId(), e.getMessage());
                return LedgerDTO.builder()
                    .id(ledger.getId())
                    .orderLogId(ledger.getOrderLogId())
                    .ticker(ledger.getTicker())
                    .stockName("N/A")
                    .transactionType(ledger.getTransactionType() != null ? ledger.getTransactionType().name() : "UNKNOWN")
                    .transactionTypeKorean(getTransactionTypeKorean(ledger.getTransactionType()))
                    .quantity(ledger.getQtyChange())
                    .price(ledger.getAmountChange())
                    .totalAmount(ledger.getCredit() != null ? ledger.getCredit() : ledger.getDebit())
                    .debit(ledger.getDebit())
                    .credit(ledger.getCredit())
                    .commission(ledger.getCommission())
                    .tax(ledger.getTax())
                    .accountNumber("N/A")
                    .memberName("N/A")
                    .counterpartyAccountNumber(ledger.getCounterpartyAccountNumber())
                    .counterpartyName(ledger.getCounterpartyName())
                    .createdAt(ledger.getCreatedAt())
                    .build();
            }
        });
    }

    private String getMemberName(MemberAccount account) {
        if (account == null || account.getMemberId() == null) {
            return "N/A";
        }
        try {
            Map<String, String> nameResponse = memberInternalClient.getMemberName(account.getMemberId());
            return nameResponse != null && nameResponse.containsKey("name") 
                ? nameResponse.get("name") 
                : "회원 " + account.getNumber().substring(Math.max(0, account.getNumber().length() - 4));
        } catch (Exception e) {
            log.warn("Failed to get member name for account {}: {}", account.getId(), e.getMessage());
            return "회원 " + account.getNumber().substring(Math.max(0, account.getNumber().length() - 4));
        }
    }

    private String getStockName(String ticker) {
        if (ticker == null || ticker.isEmpty()) {
            return "N/A";
        }
        try {
            StockInfoResDTO stockInfo = stockFeign.getStockByTicker(ticker);
            return stockInfo != null ? stockInfo.getNameKo() : ticker;
        } catch (Exception e) {
            log.warn("Failed to get stock name for ticker {}: {}", ticker, e.getMessage());
            return ticker;
        }
    }

    private String getOrderActivityTypeKorean(Side side, OrderStatus status) {
        if (status == OrderStatus.CANCELED) {
            return "주문 취소";
        }
        return side == Side.BUY ? "매수 주문" : "매도 주문";
    }

    private String getTransactionTypeKorean(TransactionType type) {
        if (type == null) {
            return "알 수 없음";
        }
        return switch (type) {
            case BUY -> "매수";
            case SELL -> "매도";
            case DEPOSIT -> "입금";
            case WITHDRAWAL -> "출금";
            case DELISTING_REFUND -> "상장폐지 환불";
            default -> type.name();
        };
    }
}

