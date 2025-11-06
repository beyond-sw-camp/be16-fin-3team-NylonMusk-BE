package com.beyond.MKX.domain.brokerage.service;

import com.beyond.MKX.domain.account.member.client.MemberInternalClient;
import com.beyond.MKX.domain.assets.dto.StockInfoResDTO;
import com.beyond.MKX.domain.assets.entity.AccountStatus;
import com.beyond.MKX.domain.assets.entity.MemberAccount;
import com.beyond.MKX.domain.assets.entity.StockHolding;
import com.beyond.MKX.domain.assets.repository.MemberAccountRepository;
import com.beyond.MKX.domain.assets.repository.StockHoldingRepository;
import com.beyond.MKX.domain.assets.service.StockFeign;
import com.beyond.MKX.domain.brokerage.dto.*;
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


    /**
     * 일일 수수료 합산 조회
     *
     * @param brokerageId 증권사 ID
     * @param date 조회 날짜 (null이면 오늘)
     * @return 일일 수수료 데이터
     */
    @Transactional(readOnly = true)
    public DailyCommissionDTO getDailyCommission(UUID brokerageId, LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);

        // 총 수수료
        Long totalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
                brokerageId, startDateTime, endDateTime);

        // 매수 수수료
        Long buyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                brokerageId, TransactionType.BUY, startDateTime, endDateTime);

        // 매도 수수료
        Long sellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                brokerageId, TransactionType.SELL, startDateTime, endDateTime);

        // 거래 건수
        Long tradeCount = ledgerRepository.getTradeCountByBrokerageIdAndDateRange(
                brokerageId, startDateTime, endDateTime);

        // 활성 거래자 수
        Long activeTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                brokerageId, startDateTime, endDateTime);

        return DailyCommissionDTO.builder()
                .date(date)
                .totalCommission(totalCommission != null ? totalCommission : 0L)
                .buyCommission(buyCommission != null ? buyCommission : 0L)
                .sellCommission(sellCommission != null ? sellCommission : 0L)
                .tradeCount(tradeCount != null ? tradeCount.intValue() : 0)
                .activeTraders(activeTraders != null ? activeTraders.intValue() : 0)
                .build();
    }

    /**
     * 기간별 수수료 조회 (7일, 30일, 90일, 365일)
     *
     * @param brokerageId 증권사 ID
     * @param days 기간 (7, 30, 90, 365)
     * @return 기간별 수수료 데이터
     */
    @Transactional(readOnly = true)
    public PeriodCommissionDTO getCommissionByPeriod(UUID brokerageId, Integer days) {
        if (days == null) {
            days = 7;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime periodStart = now.minusDays(days);
        LocalDateTime previousPeriodStart = now.minusDays(days * 2);

        // 현재 기간 수수료
        Long totalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
                brokerageId, periodStart, now);

        // 매수 수수료
        Long buyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                brokerageId, TransactionType.BUY, periodStart, now);

        // 매도 수수료
        Long sellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                brokerageId, TransactionType.SELL, periodStart, now);

        // 이전 기간 수수료 (변화율 계산용)
        Long previousCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
                brokerageId, previousPeriodStart, periodStart);

        // 변화율 계산
        double changePercent = 0.0;
        if (previousCommission != null && previousCommission > 0) {
            changePercent = ((double)(totalCommission - previousCommission) / previousCommission) * 100.0;
        } else if (totalCommission != null && totalCommission > 0) {
            changePercent = 100.0;
        }

        // 일별 상세 데이터 생성
        List<DailyCommissionDTO> dailyBreakdown = generateDailyBreakdown(brokerageId, periodStart, now);

        // 일평균 계산
        double dailyAverage = totalCommission != null ? (double) totalCommission / days : 0.0;

        return PeriodCommissionDTO.builder()
                .periodDays(days)
                .totalCommission(totalCommission != null ? totalCommission : 0L)
                .buyCommission(buyCommission != null ? buyCommission : 0L)
                .sellCommission(sellCommission != null ? sellCommission : 0L)
                .dailyAverage(dailyAverage)
                .changePercent(changePercent)
                .dailyBreakdown(dailyBreakdown)
                .build();
    }

    /**
     * 시간대별 수익 추이 조회
     *
     * @param brokerageId 증권사 ID
     * @param days 기간 (기본값: 7일)
     * @return 시간대별 수수료 추이 리스트
     */
    @Transactional(readOnly = true)
    public List<HourlyCommissionTrendDTO> getHourlyCommissionTrends(UUID brokerageId, Integer days) {
        if (days == null) {
            days = 7;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDateTime = now.minusDays(days);

        // 시간대별 집계 데이터 조회
        List<Object[]> hourlyData = ledgerRepository.getHourlyCommissionTrends(
                brokerageId, startDateTime, now);

        List<HourlyCommissionTrendDTO> trends = new ArrayList<>();
        for (Object[] row : hourlyData) {
            Integer hour = (Integer) row[0];
            Long totalCommission = (Long) row[1];
            Long buyCommission = (Long) row[2];
            Long sellCommission = (Long) row[3];
            Long tradeCount = (Long) row[4];

            // 해당 시간대의 활성 거래자 수 계산
            // 전체 기간에서 해당 시간대에 주문한 고유 계좌 수
            LocalDateTime hourStart = startDateTime.withHour(hour).withMinute(0).withSecond(0);
            LocalDateTime hourEnd = hourStart.plusHours(1);
            Long activeTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                    brokerageId, hourStart, hourEnd);

            trends.add(HourlyCommissionTrendDTO.builder()
                    .hour(hour)
                    .totalCommission(totalCommission)
                    .buyCommission(buyCommission)
                    .sellCommission(sellCommission)
                    .tradeCount(tradeCount.intValue())
                    .activeTraders(activeTraders != null ? activeTraders.intValue() : 0)
                    .build());
        }

        return trends;
    }

    /**
     * 일별 수수료 상세 데이터 생성 (헬퍼 메서드)
     */
    private List<DailyCommissionDTO> generateDailyBreakdown(
            UUID brokerageId, LocalDateTime startDateTime, LocalDateTime endDateTime) {

        // 일별 집계 데이터 조회
        List<Object[]> dailyData = ledgerRepository.getDailyCommissionBreakdown(
                brokerageId, startDateTime, endDateTime);

        List<DailyCommissionDTO> breakdown = new ArrayList<>();
        for (Object[] row : dailyData) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate date = sqlDate.toLocalDate();
            Long totalCommission = (Long) row[1];
            Long buyCommission = (Long) row[2];
            Long sellCommission = (Long) row[3];
            Long tradeCount = (Long) row[4];

            // 해당 날짜의 활성 거래자 수 계산
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
            Long activeTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                    brokerageId, dayStart, dayEnd);

            breakdown.add(DailyCommissionDTO.builder()
                    .date(date)
                    .totalCommission(totalCommission)
                    .buyCommission(buyCommission)
                    .sellCommission(sellCommission)
                    .tradeCount(tradeCount.intValue())
                    .activeTraders(activeTraders != null ? activeTraders.intValue() : 0)
                    .build());
        }

        return breakdown;
    }


    /**
     * 7일간 수수료 차트 데이터 조회
     * - 조회 날짜 포함 7일 전 데이터
     * - 1일 단위로 7개의 막대그래프 데이터
     *
     * @param brokerageId 증권사 ID
     * @param queryDate 조회 기준 날짜 (null이면 오늘)
     * @return 7일간 차트 데이터
     */
    @Transactional(readOnly = true)
    public CommissionChartDTO getCommissionChart7Days(UUID brokerageId, LocalDate queryDate) {
        if (queryDate == null) {
            queryDate = LocalDate.now();
        }

        LocalDate startDate = queryDate.minusDays(6); // 조회일 포함 7일 전
        LocalDate endDate = queryDate;

        List<CommissionChartDTO.ChartDataPoint> dataPoints = new ArrayList<>();
        Long totalCommission = 0L;

        // 7일간 각 날짜별로 데이터 생성
        for (int i = 0; i < 7; i++) {
            LocalDate targetDate = startDate.plusDays(i);
            LocalDateTime dayStart = targetDate.atStartOfDay();
            LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

            // 해당 날짜의 수수료 데이터 조회
            Long dayTotalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
                    brokerageId, dayStart, dayEnd);
            Long dayBuyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.BUY, dayStart, dayEnd);
            Long daySellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.SELL, dayStart, dayEnd);
            Long dayTradeCount = ledgerRepository.getTradeCountByBrokerageIdAndDateRange(
                    brokerageId, dayStart, dayEnd);
            Long dayActiveTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                    brokerageId, dayStart, dayEnd);

            // null 체크 및 기본값 설정
            dayTotalCommission = dayTotalCommission != null ? dayTotalCommission : 0L;
            dayBuyCommission = dayBuyCommission != null ? dayBuyCommission : 0L;
            daySellCommission = daySellCommission != null ? daySellCommission : 0L;
            dayTradeCount = dayTradeCount != null ? dayTradeCount : 0L;
            dayActiveTraders = dayActiveTraders != null ? dayActiveTraders : 0L;

            totalCommission += dayTotalCommission;

            // 라벨: "MM/dd" 형식
            String label = targetDate.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));

            dataPoints.add(CommissionChartDTO.ChartDataPoint.builder()
                    .label(label)
                    .startDate(targetDate)
                    .endDate(targetDate)
                    .totalCommission(dayTotalCommission)
                    .buyCommission(dayBuyCommission)
                    .sellCommission(daySellCommission)
                    .tradeCount(dayTradeCount.intValue())
                    .activeTraders(dayActiveTraders.intValue())
                    .build());
        }

        return CommissionChartDTO.builder()
                .period("7days")
                .startDate(startDate)
                .endDate(endDate)
                .totalCommission(totalCommission)
                .dataPointCount(7)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * 30일간 수수료 차트 데이터 조회
     * - 조회 날짜 포함 30일 전 데이터
     * - 1일 단위로 30개의 막대그래프 데이터
     *
     * @param brokerageId 증권사 ID
     * @param queryDate 조회 기준 날짜 (null이면 오늘)
     * @return 30일간 차트 데이터
     */
    @Transactional(readOnly = true)
    public CommissionChartDTO getCommissionChart30Days(UUID brokerageId, LocalDate queryDate) {
        if (queryDate == null) {
            queryDate = LocalDate.now();
        }

        LocalDate startDate = queryDate.minusDays(29); // 조회일 포함 30일 전
        LocalDate endDate = queryDate;

        List<CommissionChartDTO.ChartDataPoint> dataPoints = new ArrayList<>();
        Long totalCommission = 0L;

        // 30일간 각 날짜별로 데이터 생성
        for (int i = 0; i < 30; i++) {
            LocalDate targetDate = startDate.plusDays(i);
            LocalDateTime dayStart = targetDate.atStartOfDay();
            LocalDateTime dayEnd = targetDate.atTime(LocalTime.MAX);

            // 해당 날짜의 수수료 데이터 조회
            Long dayTotalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
                    brokerageId, dayStart, dayEnd);
            Long dayBuyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.BUY, dayStart, dayEnd);
            Long daySellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.SELL, dayStart, dayEnd);
            Long dayTradeCount = ledgerRepository.getTradeCountByBrokerageIdAndDateRange(
                    brokerageId, dayStart, dayEnd);
            Long dayActiveTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                    brokerageId, dayStart, dayEnd);

            // null 체크 및 기본값 설정
            dayTotalCommission = dayTotalCommission != null ? dayTotalCommission : 0L;
            dayBuyCommission = dayBuyCommission != null ? dayBuyCommission : 0L;
            daySellCommission = daySellCommission != null ? daySellCommission : 0L;
            dayTradeCount = dayTradeCount != null ? dayTradeCount : 0L;
            dayActiveTraders = dayActiveTraders != null ? dayActiveTraders : 0L;

            totalCommission += dayTotalCommission;

            // 라벨: "MM/dd" 형식
            String label = targetDate.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd"));

            dataPoints.add(CommissionChartDTO.ChartDataPoint.builder()
                    .label(label)
                    .startDate(targetDate)
                    .endDate(targetDate)
                    .totalCommission(dayTotalCommission)
                    .buyCommission(dayBuyCommission)
                    .sellCommission(daySellCommission)
                    .tradeCount(dayTradeCount.intValue())
                    .activeTraders(dayActiveTraders.intValue())
                    .build());
        }

        return CommissionChartDTO.builder()
                .period("30days")
                .startDate(startDate)
                .endDate(endDate)
                .totalCommission(totalCommission)
                .dataPointCount(30)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * 90일간 수수료 차트 데이터 조회
     * - 조회 날짜 포함 90일 전 데이터
     * - 3일 단위로 30개의 막대그래프 데이터
     *
     * @param brokerageId 증권사 ID
     * @param queryDate 조회 기준 날짜 (null이면 오늘)
     * @return 90일간 차트 데이터
     */
    @Transactional(readOnly = true)
    public CommissionChartDTO getCommissionChart90Days(UUID brokerageId, LocalDate queryDate) {
        if (queryDate == null) {
            queryDate = LocalDate.now();
        }

        LocalDate startDate = queryDate.minusDays(89); // 조회일 포함 90일 전
        LocalDate endDate = queryDate;

        List<CommissionChartDTO.ChartDataPoint> dataPoints = new ArrayList<>();
        Long totalCommission = 0L;

        // 90일을 3일씩 묶어서 30개 구간으로 나눔
        for (int i = 0; i < 30; i++) {
            LocalDate periodStart = startDate.plusDays(i * 3);
            LocalDate periodEnd = periodStart.plusDays(2); // 3일 구간 (시작일 + 2일)

            // 마지막 구간이 queryDate를 초과하지 않도록 조정
            if (periodEnd.isAfter(endDate)) {
                periodEnd = endDate;
            }

            LocalDateTime rangeStart = periodStart.atStartOfDay();
            LocalDateTime rangeEnd = periodEnd.atTime(LocalTime.MAX);

            // 해당 3일 구간의 수수료 데이터 조회
            Long periodTotalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
                    brokerageId, rangeStart, rangeEnd);
            Long periodBuyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.BUY, rangeStart, rangeEnd);
            Long periodSellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.SELL, rangeStart, rangeEnd);
            Long periodTradeCount = ledgerRepository.getTradeCountByBrokerageIdAndDateRange(
                    brokerageId, rangeStart, rangeEnd);
            Long periodActiveTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                    brokerageId, rangeStart, rangeEnd);

            // null 체크 및 기본값 설정
            periodTotalCommission = periodTotalCommission != null ? periodTotalCommission : 0L;
            periodBuyCommission = periodBuyCommission != null ? periodBuyCommission : 0L;
            periodSellCommission = periodSellCommission != null ? periodSellCommission : 0L;
            periodTradeCount = periodTradeCount != null ? periodTradeCount : 0L;
            periodActiveTraders = periodActiveTraders != null ? periodActiveTraders : 0L;

            totalCommission += periodTotalCommission;

            // 라벨: "MM/dd~MM/dd" 형식
            String label = String.format("%s~%s",
                    periodStart.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")),
                    periodEnd.format(java.time.format.DateTimeFormatter.ofPattern("MM/dd")));

            dataPoints.add(CommissionChartDTO.ChartDataPoint.builder()
                    .label(label)
                    .startDate(periodStart)
                    .endDate(periodEnd)
                    .totalCommission(periodTotalCommission)
                    .buyCommission(periodBuyCommission)
                    .sellCommission(periodSellCommission)
                    .tradeCount(periodTradeCount.intValue())
                    .activeTraders(periodActiveTraders.intValue())
                    .build());
        }

        return CommissionChartDTO.builder()
                .period("90days")
                .startDate(startDate)
                .endDate(endDate)
                .totalCommission(totalCommission)
                .dataPointCount(30)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * 1년간 수수료 차트 데이터 조회
     * - 조회 월 포함 1년 전 데이터
     * - 1개월 단위로 12개의 막대그래프 데이터
     *
     * @param brokerageId 증권사 ID
     * @param queryDate 조회 기준 날짜 (null이면 오늘)
     * @return 1년간 차트 데이터
     */
    @Transactional(readOnly = true)
    public CommissionChartDTO getCommissionChart365Days(UUID brokerageId, LocalDate queryDate) {
        if (queryDate == null) {
            queryDate = LocalDate.now();
        }

        // 조회 월 포함 1년 전 (11개월 전의 1일부터 시작)
        LocalDate startDate = queryDate.minusMonths(11).withDayOfMonth(1);
        LocalDate endDate = queryDate;

        List<CommissionChartDTO.ChartDataPoint> dataPoints = new ArrayList<>();
        Long totalCommission = 0L;

        // 12개월간 각 월별로 데이터 생성
        for (int i = 0; i < 12; i++) {
            LocalDate monthStart = startDate.plusMonths(i);
            LocalDate monthEnd;

            if (i == 11) {
                // 마지막 월은 queryDate까지만
                monthEnd = queryDate;
            } else {
                // 해당 월의 마지막 날
                monthEnd = monthStart.plusMonths(1).minusDays(1);
            }

            LocalDateTime rangeStart = monthStart.atStartOfDay();
            LocalDateTime rangeEnd = monthEnd.atTime(LocalTime.MAX);

            // 해당 월의 수수료 데이터 조회
            Long monthTotalCommission = ledgerRepository.getTotalCommissionByBrokerageIdAndDateRange(
                    brokerageId, rangeStart, rangeEnd);
            Long monthBuyCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.BUY, rangeStart, rangeEnd);
            Long monthSellCommission = ledgerRepository.getCommissionByBrokerageIdAndDateRange(
                    brokerageId, TransactionType.SELL, rangeStart, rangeEnd);
            Long monthTradeCount = ledgerRepository.getTradeCountByBrokerageIdAndDateRange(
                    brokerageId, rangeStart, rangeEnd);
            Long monthActiveTraders = orderLogRepository.countDistinctMemberAccountsByBrokerageIdAndDateRange(
                    brokerageId, rangeStart, rangeEnd);

            // null 체크 및 기본값 설정
            monthTotalCommission = monthTotalCommission != null ? monthTotalCommission : 0L;
            monthBuyCommission = monthBuyCommission != null ? monthBuyCommission : 0L;
            monthSellCommission = monthSellCommission != null ? monthSellCommission : 0L;
            monthTradeCount = monthTradeCount != null ? monthTradeCount : 0L;
            monthActiveTraders = monthActiveTraders != null ? monthActiveTraders : 0L;

            totalCommission += monthTotalCommission;

            // 라벨: "yyyy/MM" 형식
            String label = monthStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM"));

            dataPoints.add(CommissionChartDTO.ChartDataPoint.builder()
                    .label(label)
                    .startDate(monthStart)
                    .endDate(monthEnd)
                    .totalCommission(monthTotalCommission)
                    .buyCommission(monthBuyCommission)
                    .sellCommission(monthSellCommission)
                    .tradeCount(monthTradeCount.intValue())
                    .activeTraders(monthActiveTraders.intValue())
                    .build());
        }

        return CommissionChartDTO.builder()
                .period("365days")
                .startDate(startDate)
                .endDate(endDate)
                .totalCommission(totalCommission)
                .dataPointCount(12)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * 통합 차트 데이터 조회 메서드
     * - period 파라미터에 따라 적절한 차트 데이터 반환
     *
     * @param brokerageId 증권사 ID
     * @param period 조회 기간 ("7days", "30days", "90days", "365days")
     * @param queryDate 조회 기준 날짜 (null이면 오늘)
     * @return 기간별 차트 데이터
     */
    @Transactional(readOnly = true)
    public CommissionChartDTO getCommissionChart(UUID brokerageId, String period, LocalDate queryDate) {
        return switch (period.toLowerCase()) {
            case "7days", "7" -> getCommissionChart7Days(brokerageId, queryDate);
            case "30days", "30" -> getCommissionChart30Days(brokerageId, queryDate);
            case "90days", "90" -> getCommissionChart90Days(brokerageId, queryDate);
            case "365days", "365", "1year" -> getCommissionChart365Days(brokerageId, queryDate);
            default -> throw new IllegalArgumentException(
                    "Invalid period: " + period + ". Must be one of: 7days, 30days, 90days, 365days");
        };
    }
}

