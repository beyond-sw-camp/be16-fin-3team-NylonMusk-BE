package com.beyond.MKX.domain.tradingBot.service;

import com.beyond.MKX.common.apiResponse.CommonDTO;
import com.beyond.MKX.domain.tradingBot.entity.OrderKind;
import com.beyond.MKX.domain.tradingBot.entity.Side;
import com.beyond.MKX.domain.tradingBot.dto.OrderRequestDTO;
import com.beyond.MKX.domain.tradingBot.client.CurrentPriceClient;
import com.beyond.MKX.domain.tradingBot.client.OrderClient;
import com.beyond.MKX.domain.tradingBot.dto.CreateTradingBotConfigRequest;
import com.beyond.MKX.domain.tradingBot.dto.TradingBotConfigDTO;
import com.beyond.MKX.domain.tradingBot.dto.UpdateTradingBotStatusRequest;
import com.beyond.MKX.domain.tradingBot.entity.BotExecutionStatus;
import com.beyond.MKX.domain.tradingBot.entity.TradingBotConfig;
import com.beyond.MKX.domain.tradingBot.entity.TradingStrategy;
import com.beyond.MKX.domain.tradingBot.repository.TradingBotConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TradingBotService {

    private final CurrentPriceClient currentPriceClient;
    private final OrderClient orderClient;
    private final TradingBotConfigRepository configRepository;
    private final ObjectMapper objectMapper;

    /**
     * 봇 설정 생성
     */
    public TradingBotConfigDTO createBotConfig(CreateTradingBotConfigRequest request) {
        // buyAccountId 또는 sellAccountId 중 하나는 필수
        if (request.getBuyAccountId() == null && request.getSellAccountId() == null) {
            throw new IllegalArgumentException("buyAccountId 또는 sellAccountId 중 하나는 필수입니다.");
        }

        TradingBotConfig config = TradingBotConfig.builder()
                .ticker(request.getTicker())
                .status(request.getStatus())
                .priceLimitHigh(request.getPriceLimitHigh())
                .priceLimitLow(request.getPriceLimitLow())
                .quantity(request.getQuantity())
                .side(request.getSide()) // 하위 호환성을 위해 유지
                .orderType(request.getOrderType())
                .brokerageId(request.getBrokerageId())
                .description(request.getDescription())
                .buyAccountId(request.getBuyAccountId())
                .sellAccountId(request.getSellAccountId())
                .tradingStrategy(request.getTradingStrategy())
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
     * 자동 트레이딩 실행 (50ms마다 실행)
     */
    @Scheduled(fixedDelay = 500)
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
                    updateBotStatus(bot, BotExecutionStatus.ERROR, "예상치 못한 에러: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error in scheduled trading execution: {}", e.getMessage());
        }
    }

    /**
     * 개별 봇 주문 실행
     * buyAccountId와 sellAccountId 둘 다 활용하여 매수/매도 주문 모두 실행
     */
    private void executeOrder(TradingBotConfig bot) {
        try {
            // 1. 현재가 조회
            Long currentPrice = getCurrentPriceFromApi(bot.getTicker());
            if (currentPrice == null) {
                log.warn("Bot {}: 현재가 조회 실패 (ticker: {})", bot.getId(), bot.getTicker());
                updateBotStatus(bot, BotExecutionStatus.ERROR, "현재가 조회 실패");
                return;
            }

            // 2. 매수/매도 가격 계산 (각각 다른 가격대)
            Long buyPrice = null;
            Long sellPrice = null;
            
            if (bot.getBuyAccountId() != null) {
                buyPrice = calculateBuyPrice(currentPrice, bot.getTradingStrategy());
                if (buyPrice == null) {
                    log.warn("Bot {}: 매수 가격 계산 실패", bot.getId());
                }
            }
            
            if (bot.getSellAccountId() != null) {
                sellPrice = calculateSellPrice(currentPrice, bot.getTradingStrategy());
                if (sellPrice == null) {
                    log.warn("Bot {}: 매도 가격 계산 실패", bot.getId());
                }
            }

            // 3. 주문 수량 결정 (기본 수량 사용)
            Long quantity = bot.getQuantity().longValue();
            int successCount = 0;
            boolean hasFailure = false;

            // 4. 매수 주문 실행 (buyAccountId가 있는 경우)
            if (bot.getBuyAccountId() != null && buyPrice != null) {
                try {
                    // 30% 확률로 시장가 주문, 70% 확률로 지정가 주문
                    boolean useMarketOrder = Math.random() < 0.3;
                    executeBuyOrder(bot, buyPrice, quantity, useMarketOrder);
                    successCount++;
                } catch (Exception e) {
                    log.warn("Bot {}: 매수 주문 실패 - {}", bot.getId(), e.getMessage());
                    hasFailure = true;
                }
            }

            // 5. 매도 주문 실행 (sellAccountId가 있는 경우)
            if (bot.getSellAccountId() != null && sellPrice != null) {
                try {
                    // 30% 확률로 시장가 주문, 70% 확률로 지정가 주문
                    boolean useMarketOrder = Math.random() < 0.3;
                    executeSellOrder(bot, sellPrice, quantity, useMarketOrder);
                    successCount++;
                } catch (Exception e) {
                    log.warn("Bot {}: 매도 주문 실패 - {}", bot.getId(), e.getMessage());
                    hasFailure = true;
                }
            }

            // 6. 계좌 ID가 하나도 없는 경우
            if (bot.getBuyAccountId() == null && bot.getSellAccountId() == null) {
                log.warn("Bot {}: 매수/매도 계좌 ID가 모두 없습니다.", bot.getId());
                updateBotStatus(bot, BotExecutionStatus.ERROR, "계좌 ID 없음");
                return;
            }

            // 7. 상태 업데이트
            if (successCount > 0 && !hasFailure) {
                updateBotStatus(bot, BotExecutionStatus.SUCCESS, null);
                bot.setTotalOrderCount(bot.getTotalOrderCount() + successCount);
                bot.setConsecutiveSkipCount(0);
            } else if (hasFailure) {
                bot.setTotalSkipCount(bot.getTotalSkipCount() + 1);
                bot.setConsecutiveSkipCount(bot.getConsecutiveSkipCount() + 1);
            }

        } catch (Exception e) {
            log.error("Bot {}: 주문 실행 중 예외 발생: {}", bot.getId(), e.getMessage(), e);
            updateBotStatus(bot, BotExecutionStatus.ERROR, "주문 실행 예외: " + e.getMessage());
        }
    }

    /**
     * 매수 주문 실행
     */
    private void executeBuyOrder(TradingBotConfig bot, Long orderPrice, Long quantity, boolean useMarketOrder) {
        OrderKind orderKind = useMarketOrder ? OrderKind.MARKET : OrderKind.LIMIT;
        Long price = useMarketOrder ? null : orderPrice;
        
        OrderRequestDTO orderRequest = new OrderRequestDTO(
                orderKind,
                Side.BUY,
                bot.getBuyAccountId(),
                bot.getTicker(),
                price,
                quantity
        );

        CommonDTO response = orderClient.placeOrder(orderRequest);
        
        if (response != null && response.getResult() != null) {
            String orderTypeStr = useMarketOrder ? "시장가" : "지정가";
            log.info("✅ 매수 주문 성공! Bot: {}, Ticker: {}, Type: {}, Price: {}, Quantity: {}",
                    bot.getId(), bot.getTicker(), orderTypeStr, price != null ? price : "시장가", quantity);
        } else {
            log.warn("Bot {}: 매수 주문 응답이 비정상입니다.", bot.getId());
            throw new RuntimeException("주문 응답 비정상");
        }
    }

    /**
     * 매도 주문 실행
     */
    private void executeSellOrder(TradingBotConfig bot, Long orderPrice, Long quantity, boolean useMarketOrder) {
        OrderKind orderKind = useMarketOrder ? OrderKind.MARKET : OrderKind.LIMIT;
        Long price = useMarketOrder ? null : orderPrice;
        
        OrderRequestDTO orderRequest = new OrderRequestDTO(
                orderKind,
                Side.SELL,
                bot.getSellAccountId(),
                bot.getTicker(),
                price,
                quantity
        );

        CommonDTO response = orderClient.placeOrder(orderRequest);
        
        if (response != null && response.getResult() != null) {
            String orderTypeStr = useMarketOrder ? "시장가" : "지정가";
            log.info("✅ 매도 주문 성공! Bot: {}, Ticker: {}, Type: {}, Price: {}, Quantity: {}",
                    bot.getId(), bot.getTicker(), orderTypeStr, price != null ? price : "시장가", quantity);
        } else {
            log.warn("Bot {}: 매도 주문 응답이 비정상입니다.", bot.getId());
            throw new RuntimeException("주문 응답 비정상");
        }
    }

    /**
     * 주문 실패 시 적응형 처리
     */
    private void handleOrderFailure(TradingBotConfig bot, Exception e, OrderRequestDTO originalRequest, Long orderPrice) {
        String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
        log.warn("Bot {}: 주문 실패 - {}", bot.getId(), errorMessage);

        // 에러 메시지로 적응형 처리 결정
        if (errorMessage.contains("잔고") || errorMessage.contains("부족")) {
            // 잔고 부족 - 수량 조정 시도
            if (originalRequest.side() == Side.BUY) {
                Long adjustedQuantity = adjustBuyQuantity(bot, orderPrice, originalRequest.quantity());
                if (adjustedQuantity != null && adjustedQuantity > 0) {
                    try {
                        OrderRequestDTO adjustedRequest = new OrderRequestDTO(
                                originalRequest.orderKind(),
                                originalRequest.side(),
                                originalRequest.accountId(),
                                originalRequest.ticker(),
                                originalRequest.price(),
                                adjustedQuantity
                        );
                        CommonDTO response = orderClient.placeOrder(adjustedRequest);
                        
                        if (response != null && response.getResult() != null) {
                            log.info("✅ 수량 조정 후 주문 성공! Bot: {}, Adjusted Quantity: {}", 
                                    bot.getId(), adjustedQuantity);
                            updateBotStatus(bot, BotExecutionStatus.ADJUSTED_QUANTITY, 
                                    "수량 조정: " + originalRequest.quantity() + " -> " + adjustedQuantity);
                            bot.setTotalOrderCount(bot.getTotalOrderCount() + 1);
                            bot.setConsecutiveSkipCount(0);
                            return;
                        }
                    } catch (Exception ex) {
                        log.warn("Bot {}: 수량 조정 후 주문도 실패", bot.getId());
                    }
                }
            }
            updateBotStatus(bot, BotExecutionStatus.INSUFFICIENT_BALANCE, errorMessage);
            
        } else if (errorMessage.contains("보유") || errorMessage.contains("주식") || errorMessage.contains("수량")) {
            // 보유주식 부족 - 수량 조정 시도
            if (originalRequest.side() == Side.SELL) {
                Long adjustedQuantity = adjustSellQuantity(bot, originalRequest.quantity());
                if (adjustedQuantity != null && adjustedQuantity > 0) {
                    try {
                        OrderRequestDTO adjustedRequest = new OrderRequestDTO(
                                originalRequest.orderKind(),
                                originalRequest.side(),
                                originalRequest.accountId(),
                                originalRequest.ticker(),
                                originalRequest.price(),
                                adjustedQuantity
                        );
                        CommonDTO response = orderClient.placeOrder(adjustedRequest);
                        
                        if (response != null && response.getResult() != null) {
                            log.info("✅ 수량 조정 후 주문 성공! Bot: {}, Adjusted Quantity: {}", 
                                    bot.getId(), adjustedQuantity);
                            updateBotStatus(bot, BotExecutionStatus.ADJUSTED_QUANTITY, 
                                    "수량 조정: " + originalRequest.quantity() + " -> " + adjustedQuantity);
                            bot.setTotalOrderCount(bot.getTotalOrderCount() + 1);
                            bot.setConsecutiveSkipCount(0);
                            return;
                        }
                    } catch (Exception ex) {
                        log.warn("Bot {}: 수량 조정 후 주문도 실패", bot.getId());
                    }
                }
            }
            updateBotStatus(bot, BotExecutionStatus.INSUFFICIENT_STOCK, errorMessage);
            
        } else {
            // 기타 에러
            updateBotStatus(bot, BotExecutionStatus.ERROR, errorMessage);
        }

        // 스킵 처리
        bot.setTotalSkipCount(bot.getTotalSkipCount() + 1);
        bot.setConsecutiveSkipCount(bot.getConsecutiveSkipCount() + 1);
        log.info("⏳ Bot {}: 주문 스킵 (연속 스킵: {}회)", bot.getId(), bot.getConsecutiveSkipCount());
    }

    /**
     * 매수 주문 수량 조정 (잔고 기반)
     * 실제로는 주문 API가 에러를 반환하므로, 간단히 반으로 줄여서 재시도
     */
    private Long adjustBuyQuantity(TradingBotConfig bot, Long price, Long originalQuantity) {
        // 간단한 로직: 수량을 절반으로 줄임
        Long adjusted = originalQuantity / 2;
        return adjusted >= 1 ? adjusted : null;
    }

    /**
     * 매도 주문 수량 조정 (보유주식 기반)
     * 실제로는 주문 API가 에러를 반환하므로, 간단히 반으로 줄여서 재시도
     */
    private Long adjustSellQuantity(TradingBotConfig bot, Long originalQuantity) {
        // 간단한 로직: 수량을 절반으로 줄임
        Long adjusted = originalQuantity / 2;
        return adjusted >= 1 ? adjusted : null;
    }

    /**
     * 현재가 조회 (FeignClient 사용)
     */
    private Long getCurrentPriceFromApi(String ticker) {
        try {
            CommonDTO response = currentPriceClient.getCurrentPrice(ticker);
            log.debug("현재가 조회 응답 (ticker: {}): status_code={}, result 타입={}", 
                    ticker, 
                    response != null ? response.getStatus_code() : null,
                    response != null && response.getResult() != null ? response.getResult().getClass().getName() : null);
            
            if (response == null) {
                log.warn("현재가 조회 실패 (ticker: {}): 응답이 null입니다.", ticker);
                return null;
            }
            
            if (response.getResult() == null) {
                log.warn("현재가 조회 실패 (ticker: {}): result가 null입니다. status_code={}, status_message={}", 
                        ticker, response.getStatus_code(), response.getStatus_message());
                return null;
            }
            
            // result가 Map인 경우 직접 처리
            if (response.getResult() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> priceMap = (Map<String, Object>) response.getResult();
                Object priceObj = priceMap.get("price");
                
                if (priceObj == null) {
                    log.warn("현재가 조회 실패 (ticker: {}): result Map에 'price' 필드가 없습니다. Map keys: {}", 
                            ticker, priceMap.keySet());
                    return null;
                }
                
                if (priceObj instanceof Number) {
                    Long price = ((Number) priceObj).longValue();
                    log.debug("현재가 조회 성공 (ticker: {}): price={}", ticker, price);
                    return price;
                } else {
                    log.warn("현재가 조회 실패 (ticker: {}): price 필드가 Number 타입이 아닙니다. 타입: {}", 
                            ticker, priceObj.getClass().getName());
                    return null;
                }
            }
            
            // result가 Map이 아닌 경우 ObjectMapper를 사용하여 변환 시도
            try {
                Map<String, Object> priceMap = objectMapper.convertValue(response.getResult(), Map.class);
                Object priceObj = priceMap.get("price");
                if (priceObj instanceof Number) {
                    Long price = ((Number) priceObj).longValue();
                    log.debug("현재가 조회 성공 (ObjectMapper 사용, ticker: {}): price={}", ticker, price);
                    return price;
                }
            } catch (Exception mapperEx) {
                log.warn("현재가 조회 실패 (ticker: {}): ObjectMapper 변환 실패 - {}", ticker, mapperEx.getMessage());
            }
            
            log.warn("현재가 조회 실패 (ticker: {}): result를 처리할 수 없습니다. result 타입: {}", 
                    ticker, response.getResult().getClass().getName());
            return null;
            
        } catch (Exception e) {
            log.error("현재가 조회 실패 (ticker: {}): 예외 발생 - {}", ticker, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 매수 가격 계산 (현재가보다 낮게 설정하여 체결 유도)
     * 10원 단위로 가격 변동
     */
    private Long calculateBuyPrice(Long currentPrice, TradingStrategy strategy) {
        if (currentPrice == null || currentPrice <= 0) {
            return null;
        }

        // 기본: 현재가보다 10원 ~ 100원 낮게 매수
        long baseOffset = 10 + (long)(Math.random() * 90); // 10 ~ 100원
        
        if (strategy == null) {
            return Math.max(currentPrice - baseOffset, 1L);
        }

        long offset;
        switch (strategy) {
            case SIDEWAYS:
                // 횡보: 현재가 -10원 ~ -100원
                offset = 10 + (long)(Math.random() * 90);
                break;
            case DOWNWARD:
                // 하락: 현재가 -20원 ~ -200원
                offset = 20 + (long)(Math.random() * 180);
                break;
            case UPWARD:
                // 상승: 현재가 -5원 ~ -50원 (약간만 낮게)
                offset = 5 + (long)(Math.random() * 45);
                break;
            case SHARP_DOWN:
                // 급하락: 현재가 -50원 ~ -500원
                offset = 50 + (long)(Math.random() * 450);
                break;
            case SHARP_UP:
                // 급상승: 현재가 -5원 ~ -30원 (매우 작게)
                offset = 5 + (long)(Math.random() * 25);
                break;
            default:
                offset = 10 + (long)(Math.random() * 90);
        }

        long calculatedPrice = currentPrice - offset;
        // 가격은 최소 1원 이상
        return Math.max(calculatedPrice, 1L);
    }

    /**
     * 매도 가격 계산 (현재가보다 높게 설정하여 체결 유도)
     * 10원 단위로 가격 변동
     */
    private Long calculateSellPrice(Long currentPrice, TradingStrategy strategy) {
        if (currentPrice == null || currentPrice <= 0) {
            return null;
        }

        // 기본: 현재가보다 10원 ~ 100원 높게 매도
        long baseOffset = 10 + (long)(Math.random() * 90); // 10 ~ 100원
        
        if (strategy == null) {
            return currentPrice + baseOffset;
        }

        long offset;
        switch (strategy) {
            case SIDEWAYS:
                // 횡보: 현재가 +10원 ~ +100원
                offset = 10 + (long)(Math.random() * 90);
                break;
            case DOWNWARD:
                // 하락: 현재가 +5원 ~ +50원 (약간만 높게)
                offset = 5 + (long)(Math.random() * 45);
                break;
            case UPWARD:
                // 상승: 현재가 +20원 ~ +200원
                offset = 20 + (long)(Math.random() * 180);
                break;
            case SHARP_DOWN:
                // 급하락: 현재가 +5원 ~ +30원 (매우 작게)
                offset = 5 + (long)(Math.random() * 25);
                break;
            case SHARP_UP:
                // 급상승: 현재가 +50원 ~ +500원
                offset = 50 + (long)(Math.random() * 450);
                break;
            default:
                offset = 10 + (long)(Math.random() * 90);
        }

        return currentPrice + offset;
    }

    /**
     * 봇 상태 업데이트
     */
    private void updateBotStatus(TradingBotConfig bot, BotExecutionStatus status, String errorMessage) {
        bot.setLastExecutionStatus(status);
        bot.setLastErrorMessage(errorMessage);
        bot.setLastExecutionTime(LocalDateTime.now());
        configRepository.save(bot);
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
                .buyAccountId(config.getBuyAccountId())
                .sellAccountId(config.getSellAccountId())
                .tradingStrategy(config.getTradingStrategy())
                .lastExecutionStatus(config.getLastExecutionStatus())
                .lastErrorMessage(config.getLastErrorMessage())
                .lastExecutionTime(config.getLastExecutionTime())
                .consecutiveSkipCount(config.getConsecutiveSkipCount())
                .totalOrderCount(config.getTotalOrderCount())
                .totalSkipCount(config.getTotalSkipCount())
                .build();
    }
}
