package com.beyond.MKX.domain.tradingBot.service;

import com.beyond.MKX.domain.order.client.MatchingEngineClient;
import com.beyond.MKX.domain.order.entity.OrderEvent;
import com.beyond.MKX.domain.tradingBot.dto.CreateTradingBotConfigRequest;
import com.beyond.MKX.domain.tradingBot.dto.TradingBotConfigDTO;
import com.beyond.MKX.domain.tradingBot.dto.UpdateTradingBotStatusRequest;
import com.beyond.MKX.domain.tradingBot.entity.TradingBotConfig;
import com.beyond.MKX.domain.tradingBot.repository.TradingBotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradingBotService {

    private final MatchingEngineClient matchingEngineClient;
    private final TradingBotConfigRepository configRepository;

    /**
     * 봇 설정 생성
     */
    public TradingBotConfigDTO createBotConfig(CreateTradingBotConfigRequest request) {
        TradingBotConfig config = TradingBotConfig.builder()
                .ticker(request.getTicker())
                .status(request.getStatus())
                .priceLimitHigh(request.getPriceLimitHigh())
                .priceLimitLow(request.getPriceLimitLow())
                .quantity(request.getQuantity())
                .side(request.getSide())
                .orderType(request.getOrderType())
                .brokerageId(request.getBrokerageId())
                .description(request.getDescription())
                .isActive(true)
                .build();

        TradingBotConfig savedConfig = configRepository.save(config);
        log.info("Trading bot config created: {}", savedConfig.getId());

        return convertToDTO(savedConfig);
    }

    /**
     * 봇 상태 업데이트
     */
    public TradingBotConfigDTO updateBotStatus(UUID id, UpdateTradingBotStatusRequest request) {
        TradingBotConfig config = configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trading bot config not found: " + id));

        config.setStatus(request.getStatus());
        TradingBotConfig savedConfig = configRepository.save(config);

        log.info("Trading bot status updated: {} -> {}", id, request.getStatus());
        return convertToDTO(savedConfig);
    }

    /**
     * 봇 비활성화
     */
    public void deactivateBot(UUID id) {
        TradingBotConfig config = configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trading bot config not found: " + id));

        config.setIsActive(false);
        configRepository.save(config);

        log.info("Trading bot deactivated: {}", id);
    }

    /**
     * 활성화된 모든 봇 설정 조회
     */
    @Transactional(readOnly = true)
    public List<TradingBotConfigDTO> getAllActiveBotConfigs() {
        return configRepository.findByIsActiveTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 특정 봇 설정 조회
     */
    @Transactional(readOnly = true)
    public TradingBotConfigDTO getBotConfig(UUID id) {
        TradingBotConfig config = configRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Trading bot config not found: " + id));

        return convertToDTO(config);
    }

    /**
     * 자동 트레이딩 실행 (5초마다 실행)
     */
    @Scheduled(fixedDelay = 5000)
    public void executeTrading() {
        try {
            List<TradingBotConfig> activeBots = configRepository.findByStatusAndIsActiveTrue("START");

            if (activeBots.isEmpty()) {
                return;
            }

            log.debug("Executing trading for {} active bots", activeBots.size());

            for (TradingBotConfig bot : activeBots) {
                try {
                    executeOrder(bot);
                } catch (Exception e) {
                    log.error("Failed to execute order for bot {}: {}", bot.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduled trading execution: {}", e.getMessage());
        }
    }

    /**
     * 개별 봇 주문 실행
     */
    private void executeOrder(TradingBotConfig bot) {
        // 현재 시장가격 조회 (테스트용)
        Long currentPrice = getCurrentPrice(bot.getTicker());

        log.info("Bot {} checking price: {} (range: {} ~ {})",
                bot.getId(), currentPrice, bot.getPriceLimitLow(), bot.getPriceLimitHigh());

        // 가격 범위 체크
        if (currentPrice >= bot.getPriceLimitLow() &&
                currentPrice <= bot.getPriceLimitHigh()) {

            OrderEvent orderEvent = OrderEvent.builder()
                    .brokerageId(bot.getBrokerageId())
                    .orderId(UUID.randomUUID().toString())
                    .ticker(bot.getTicker())
                    .side(bot.getSide())
                    .orderType(bot.getOrderType())
                    .price(currentPrice)
                    .quantity(bot.getQuantity())
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                matchingEngineClient.sendOrder(orderEvent);
                log.info("✅ ORDER SENT! Bot: {}, Ticker: {}, Price: {}, Quantity: {}, Side: {}",
                        bot.getId(), bot.getTicker(), currentPrice, bot.getQuantity(), bot.getSide());
            } catch (Exception e) {
                log.error("❌ Failed to send order to matching engine for bot {}: {}", bot.getId(), e.getMessage());
            }
        } else {
            log.info("⏳ Price {} not in range [{}, {}] for bot {} - waiting...",
                    currentPrice, bot.getPriceLimitLow(), bot.getPriceLimitHigh(), bot.getId());
        }
    }

    // 가격 변동 시뮬레이션을 위한 상태 저장
    private final Map<String, PriceState> priceStates = new ConcurrentHashMap<>();

    /**
     * 가격 상태 클래스
     */
    private static class PriceState {
        private long currentPrice;
        private long basePrice;
        private int trendDirection; // -1: 하락, 0: 횡보, 1: 상승
        private int volatility; // 변동성 (1-5)

        public PriceState(long basePrice) {
            this.basePrice = basePrice;
            this.currentPrice = basePrice;
            this.trendDirection = 0;
            this.volatility = 3;
        }
    }

    /**
     * 다이나믹한 가격 생성 (봇들의 거래에 따라 변동)
     */
    private Long getCurrentPrice(String ticker) {
        PriceState state = priceStates.computeIfAbsent(ticker, k -> {
            // 종목별 기본 가격 설정 (실제 종목 데이터 기반)
            long basePrice = getBasePrice(k);
            return new PriceState(basePrice);
        });

        // 봇들의 거래 활동에 따른 가격 변동 시뮬레이션
        simulatePriceMovement(state, ticker);

        return state.currentPrice;
    }

    /**
     * 가격 변동 시뮬레이션
     */
    private void simulatePriceMovement(PriceState state, String ticker) {
        // 현재 활성화된 봇들의 거래 패턴 분석
        List<TradingBotConfig> activeBots = configRepository.findByTickerAndStatusAndIsActiveTrue(ticker, "START");

        int buyBots = (int) activeBots.stream().filter(bot -> "BUY".equals(bot.getSide())).count();
        int sellBots = (int) activeBots.stream().filter(bot -> "SELL".equals(bot.getSide())).count();

        // 매수 봇이 많으면 상승 압력, 매도 봇이 많으면 하락 압력
        int pressure = buyBots - sellBots;

        // 트렌드 방향 결정
        if (pressure > 0) {
            state.trendDirection = Math.min(state.trendDirection + 1, 2);
        } else if (pressure < 0) {
            state.trendDirection = Math.max(state.trendDirection - 1, -2);
        } else {
            // 횡보 유지
            state.trendDirection = state.trendDirection > 0 ? Math.max(state.trendDirection - 1, 0)
                    : Math.min(state.trendDirection + 1, 0);
        }

        // 가격 변동 계산
        double volatilityFactor = state.volatility * 0.01; // 1-5% 변동
        double trendFactor = state.trendDirection * 0.005; // 트렌드에 따른 변동

        // 랜덤 변동 + 트렌드 + 거래 압력
        double randomChange = (Math.random() - 0.5) * volatilityFactor;
        double trendChange = trendFactor;
        double pressureChange = pressure * 0.001; // 거래 압력에 따른 변동

        double totalChange = randomChange + trendChange + pressureChange;

        // 가격 업데이트
        long priceChange = (long) (state.currentPrice * totalChange);
        state.currentPrice = (long) Math.max(state.currentPrice + priceChange, state.basePrice * 0.5); // 최소 50% 이상 유지

        // 변동성 조정 (거래량이 많을수록 변동성 증가)
        int totalBots = buyBots + sellBots;
        state.volatility = Math.min(Math.max(totalBots, 1), 5);

        log.debug("Price simulation for {}: price={}, trend={}, pressure={}, volatility={}",
                ticker, state.currentPrice, state.trendDirection, pressure, state.volatility);
    }

    /**
     * 다이나믹 봇 생성 - 자동으로 매수/매도 봇들을 생성하여 횡보 시뮬레이션
     */
    public void createDynamicBots(String ticker) {
        log.info("🚀 Creating dynamic bots for ticker: {}", ticker);

        // 기존 봇들 비활성화
        List<TradingBotConfig> existingBots = configRepository.findByTickerAndIsActiveTrue(ticker);
        existingBots.forEach(bot -> {
            bot.setIsActive(false);
            configRepository.save(bot);
        });

        // 매수 봇들 생성 (다양한 가격대)
        createBuyBots(ticker);

        // 매도 봇들 생성 (다양한 가격대)
        createSellBots(ticker);

        log.info("✅ Dynamic bots created for ticker: {}", ticker);
    }

    /**
     * 간단한 매수/매도 봇 생성 (테스트용)
     */
    public void createSimpleBots(String ticker) {
        log.info("🚀 Creating simple bots for ticker: {}", ticker);

        // 기존 봇들 삭제
        List<TradingBotConfig> existingBots = configRepository.findByTickerAndIsActiveTrue(ticker);
        existingBots.forEach(bot -> {
            bot.setIsActive(false);
            configRepository.save(bot);
        });

        // 간단한 매수/매도 봇 생성
        long basePrice = getBasePrice(ticker);
        
        // 매수 봇 (기본가 -10% ~ 기본가)
        CreateTradingBotConfigRequest buyRequest = new CreateTradingBotConfigRequest();
        buyRequest.setTicker(ticker);
        buyRequest.setStatus("START");
        buyRequest.setPriceLimitHigh(basePrice);
        buyRequest.setPriceLimitLow((long)(basePrice * 0.9));
        buyRequest.setQuantity(BigDecimal.valueOf(10));
        buyRequest.setSide("BUY");
        buyRequest.setOrderType("LIMIT");
        buyRequest.setBrokerageId("SIMPLE_BROKER");
        buyRequest.setDescription("간단 매수 봇");
        createBotConfig(buyRequest);
        
        // 매도 봇 (기본가 ~ 기본가 +10%)
        CreateTradingBotConfigRequest sellRequest = new CreateTradingBotConfigRequest();
        sellRequest.setTicker(ticker);
        sellRequest.setStatus("START");
        sellRequest.setPriceLimitHigh((long)(basePrice * 1.1));
        sellRequest.setPriceLimitLow(basePrice);
        sellRequest.setQuantity(BigDecimal.valueOf(10));
        sellRequest.setSide("SELL");
        sellRequest.setOrderType("LIMIT");
        sellRequest.setBrokerageId("SIMPLE_BROKER");
        sellRequest.setDescription("간단 매도 봇");
        createBotConfig(sellRequest);

        log.info("✅ Simple bots created for ticker: {}", ticker);
    }

    /**
     * 매수 봇들 생성
     */
    private void createBuyBots(String ticker) {
        long basePrice = getBasePrice(ticker);

        // 현실적인 가격 범위로 매수 봇 생성
        createBotDirect(ticker, "BUY", 40000, 45000, 5, "저가 매수 봇");
        createBotDirect(ticker, "BUY", 42500, 47500, 8, "중저가 매수 봇");
        createBotDirect(ticker, "BUY", 45000, 50000, 10, "중가 매수 봇");
        createBotDirect(ticker, "BUY", 47500, 52500, 6, "중고가 매수 봇");
        createBotDirect(ticker, "BUY", 50000, 55000, 4, "고가 매수 봇");
    }

    /**
     * 매도 봇들 생성
     */
    private void createSellBots(String ticker) {
        long basePrice = getBasePrice(ticker);

        // 현실적인 가격 범위로 매도 봇 생성
        createBotDirect(ticker, "SELL", 45000, 50000, 5, "저가 매도 봇");
        createBotDirect(ticker, "SELL", 47500, 52500, 8, "중저가 매도 봇");
        createBotDirect(ticker, "SELL", 50000, 55000, 10, "중가 매도 봇");
        createBotDirect(ticker, "SELL", 52500, 57500, 6, "중고가 매도 봇");
        createBotDirect(ticker, "SELL", 55000, 60000, 4, "고가 매도 봇");
    }

    /**
     * 직접 가격을 받아서 봇 생성
     */
    private void createBotDirect(String ticker, String side, long priceLow, long priceHigh, int quantity, String description) {
        TradingBotConfig config = TradingBotConfig.builder()
                .ticker(ticker)
                .status("START")
                .priceLimitLow(priceLow)
                .priceLimitHigh(priceHigh)
                .quantity(BigDecimal.valueOf(quantity))
                .side(side)
                .orderType("LIMIT")
                .brokerageId("DYNAMIC_BROKER")
                .description(description)
                .isActive(true)
                .build();

        configRepository.save(config);
        log.info("Created {} bot: {} (range: {} ~ {})", side, description, priceLow, priceHigh);
    }

    /**
     * 개별 봇 생성
     */
    private void createBot(String ticker, String side, double lowRatio, double highRatio, int quantity, String description) {
        long basePrice = getBasePrice(ticker);

        TradingBotConfig config = TradingBotConfig.builder()
                .ticker(ticker)
                .status("START")
                .priceLimitLow((long) (basePrice * lowRatio))
                .priceLimitHigh((long) (basePrice * highRatio))
                .quantity(BigDecimal.valueOf(quantity))
                .side(side)
                .orderType("LIMIT")
                .brokerageId("DYNAMIC_BROKER")
                .description(description)
                .isActive(true)
                .build();

        configRepository.save(config);
        log.info("Created {} bot: {} (range: {} ~ {})", side, description, config.getPriceLimitLow(), config.getPriceLimitHigh());
    }

    /**
     * 종목별 기본 가격 반환 (실제 종목 데이터 기반)
     */
    private long getBasePrice(String ticker) {
        return switch (ticker) {
            // 실제 종목들
            case "MKX001" -> 50000;  // 엠케이에스 원 - 중소형주
            case "MKX002" -> 35000;  // 엠케이에스 투 - 소형주
            case "MKX003" -> 75000;  // 엠케이에스 쓰리 - 중형주 (SUSPENDED)
            case "MKX004" -> 25000;  // 엠케이에스 포 - 소형주
            case "MKX005" -> 40000;  // 엠케이에스 파이브 - 중소형주
            case "MKX006" -> 120000; // 엠케이에스 식스 - 대형주
            case "MKX007" -> 60000;  // 엠케이에스 세븐 - 중형주 (SUSPENDED)
            case "MKX008" -> 85000;  // 엠케이에스 에이트 - 중형주
            case "MKX009" -> 30000;  // 엠케이에스 나인 - 소형주
            case "MKX010" -> 95000;  // 엠케이에스 텐 - 중형주

            // 기존 테스트용 종목들
            case "005930" -> 70000;  // 삼성전자
            case "000660" -> 95000;  // SK하이닉스
            case "035420" -> 175000; // NAVER
            case "207940" -> 650000; // 삼성바이오로직스

            default -> 50000; // 기본값
        };
    }

    /**
     * 엔티티를 DTO로 변환
     */
    private TradingBotConfigDTO convertToDTO(TradingBotConfig config) {
        return TradingBotConfigDTO.builder()
                .id(config.getId())
                .ticker(config.getTicker())
                .status(config.getStatus())
                .priceLimitHigh(config.getPriceLimitHigh())
                .priceLimitLow(config.getPriceLimitLow())
                .quantity(config.getQuantity())
                .side(config.getSide())
                .orderType(config.getOrderType())
                .brokerageId(config.getBrokerageId())
                .isActive(config.getIsActive())
                .description(config.getDescription())
                .build();
    }
}
