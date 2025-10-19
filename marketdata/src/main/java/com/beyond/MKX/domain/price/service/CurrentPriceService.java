package com.beyond.MKX.domain.price.service;

import com.beyond.MKX.domain.execution.dto.ExecutionEventDTO;
import com.beyond.MKX.domain.orderbook.entity.OrderBook;
import com.beyond.MKX.domain.orderbook.service.OrderBookService;
import com.beyond.MKX.domain.price.entity.CurrentPrice;
import com.beyond.MKX.domain.price.websocket.CurrentPriceWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * 현재가 관리 서비스
 * 
 * 증권거래소의 체결 메커니즘과 동일하게 주가 변동을 관리
 * - 체결가가 현재가가 됨
 * - 호가창의 최우선 호가로 시세 예측
 * - 전일대비 등락률 계산
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CurrentPriceService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OrderBookService orderBookService;
    private final CurrentPriceWebSocketHandler priceWebSocketHandler;
    private final ObjectMapper objectMapper;

    // Redis key prefix
    private static final String PRICE_KEY_PREFIX = "price:";
    private static final String PREV_CLOSE_KEY_PREFIX = "price:prev_close:";
    
    // 현재가 데이터 TTL (1시간)
    private static final long PRICE_TTL_MINUTES = 60;

    /**
     * 체결 이벤트 발생 시 현재가 업데이트
     * 실제 증권거래소와 동일: 체결가 = 현재가
     */
    public void updateCurrentPrice(ExecutionEventDTO execution) {
        try {
            String ticker = execution.getTicker();
            
            // 기존 현재가 조회
            CurrentPrice currentPrice = getCurrentPrice(ticker);
            
            // 전일 종가가 없으면 현재 가격을 전일 종가로 설정 (최초 거래)
            if (currentPrice == null) {
                currentPrice = CurrentPrice.builder()
                        .ticker(ticker)
                        .price(execution.getPrice())
                        .prevClose(execution.getPrice())
                        .open(execution.getPrice())
                        .high(execution.getPrice())
                        .low(execution.getPrice())
                        .volume(BigDecimal.ZERO)
                        .change(0L)
                        .changeRate(BigDecimal.ZERO)
                        .timestamp(Instant.ofEpochMilli(execution.getTimestamp()))
                        .build();
            }
            
            // 현재가 업데이트 (체결가가 현재가)
            long prevPrice = currentPrice.getPrice();
            currentPrice.setPrice(execution.getPrice());
            currentPrice.setTimestamp(Instant.ofEpochMilli(execution.getTimestamp()));
            
            // 당일 고가/저가 업데이트
            if (execution.getPrice() > currentPrice.getHigh()) {
                currentPrice.setHigh(execution.getPrice());
            }
            if (execution.getPrice() < currentPrice.getLow() || currentPrice.getLow() == 0) {
                currentPrice.setLow(execution.getPrice());
            }
            
            // 거래량 누적
            currentPrice.setVolume(currentPrice.getVolume().add(execution.getQuantity()));
            
            // 전일대비 등락 계산
            long change = currentPrice.getPrice() - currentPrice.getPrevClose();
            currentPrice.setChange(change);
            
            // 등락률 계산 (%)
            if (currentPrice.getPrevClose() != 0) {
                BigDecimal changeRate = BigDecimal.valueOf(change)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(currentPrice.getPrevClose()), 2, RoundingMode.HALF_UP);
                currentPrice.setChangeRate(changeRate);
            }
            
            // 호가창의 최우선 호가 정보 업데이트는 생략 (성능 최적화)
            // 호가는 WebSocket으로 별도 조회 가능
            
            // Redis에 저장
            saveCurrentPrice(currentPrice);
            
            // WebSocket으로 실시간 전송
            priceWebSocketHandler.broadcastPrice(ticker, currentPrice);
            
            log.debug("[PRICE/UPDATE] ticker={}, price={} ({}{}), volume={}", 
                    ticker, currentPrice.getPrice(), 
                    change >= 0 ? "+" : "", change,
                    currentPrice.getVolume());
            
        } catch (Exception e) {
            log.error("Failed to update current price", e);
        }
    }

    /**
     * 호가창의 최우선 호가 업데이트 (필요 시에만 호출)
     * 증권거래소에서 호가창을 보고 다음 체결가를 예측하는 것과 동일
     * 
     * 성능 최적화: 체결마다 호출하지 말고, 필요할 때만 호출하세요.
     */
    public void updateBestBidAsk(String ticker, CurrentPrice currentPrice) {
        try {
            OrderBook orderBook = orderBookService.getOrderBook(ticker);
            
            // 최우선 매수호가 (가장 높은 매수 가격)
            OrderBook.OrderBookEntry bestBid = orderBook.getBestBid();
            if (bestBid != null) {
                currentPrice.setBestBid(bestBid.getPrice());
                currentPrice.setBestBidQuantity(bestBid.getQuantity());
            } else {
                currentPrice.setBestBid(null);
                currentPrice.setBestBidQuantity(null);
            }
            
            // 최우선 매도호가 (가장 낮은 매도 가격)
            OrderBook.OrderBookEntry bestAsk = orderBook.getBestAsk();
            if (bestAsk != null) {
                currentPrice.setBestAsk(bestAsk.getPrice());
                currentPrice.setBestAskQuantity(bestAsk.getQuantity());
            } else {
                currentPrice.setBestAsk(null);
                currentPrice.setBestAskQuantity(null);
            }
            
            // 스프레드 (매도호가 - 매수호가)
            currentPrice.setSpread(orderBook.getSpread());
            
        } catch (Exception e) {
            log.warn("Failed to update best bid/ask for ticker: {}", ticker, e);
        }
    }

    /**
     * 현재가 조회 (Redis에서)
     */
    public CurrentPrice getCurrentPrice(String ticker) {
        try {
            String redisKey = buildRedisKey(ticker);
            Object data = redisTemplate.opsForValue().get(redisKey);
            
            if (data == null) {
                return null;
            }
            
            // 이미 CurrentPrice 타입인 경우
            if (data instanceof CurrentPrice) {
                return (CurrentPrice) data;
            }
            
            // LinkedHashMap 등 다른 타입인 경우 ObjectMapper로 변환
            return objectMapper.convertValue(data, CurrentPrice.class);
            
        } catch (Exception e) {
            log.error("Failed to get current price from Redis: ticker={}", ticker, e);
            return null;
        }
    }

    /**
     * 현재가 저장 (Redis에)
     */
    private void saveCurrentPrice(CurrentPrice currentPrice) {
        try {
            String redisKey = buildRedisKey(currentPrice.getTicker());
            redisTemplate.opsForValue().set(redisKey, currentPrice, 
                    PRICE_TTL_MINUTES, TimeUnit.MINUTES);
            
            log.debug("Saved current price to Redis: ticker={}, price={}", 
                    currentPrice.getTicker(), currentPrice.getPrice());
            
        } catch (Exception e) {
            log.error("Failed to save current price to Redis: ticker={}", 
                    currentPrice.getTicker(), e);
        }
    }

    /**
     * 전일 종가 설정 (장 마감 시 호출)
     * 다음 거래일의 등락 계산을 위해 현재가를 전일종가로 저장
     */
    public void setPrevClosePrice(String ticker) {
        try {
            CurrentPrice currentPrice = getCurrentPrice(ticker);
            if (currentPrice != null) {
                // 현재가를 전일종가로 저장
                String prevCloseKey = PREV_CLOSE_KEY_PREFIX + ticker;
                redisTemplate.opsForValue().set(prevCloseKey, currentPrice.getPrice(), 
                        365, TimeUnit.DAYS);
                
                log.info("Set prev close price: ticker={}, prevClose={}", 
                        ticker, currentPrice.getPrice());
            }
        } catch (Exception e) {
            log.error("Failed to set prev close price: ticker={}", ticker, e);
        }
    }

    /**
     * 전일 종가 조회
     */
    public Long getPrevClosePrice(String ticker) {
        try {
            String prevCloseKey = PREV_CLOSE_KEY_PREFIX + ticker;
            Object data = redisTemplate.opsForValue().get(prevCloseKey);
            
            if (data instanceof Number) {
                return ((Number) data).longValue();
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get prev close price: ticker={}", ticker, e);
            return null;
        }
    }

    /**
     * 장 시작 시 초기화 (시가 설정)
     */
    public void initializeDailyPrice(String ticker) {
        try {
            CurrentPrice currentPrice = getCurrentPrice(ticker);
            if (currentPrice != null) {
                // 전일 종가를 현재가의 prevClose로 설정
                Long prevClose = getPrevClosePrice(ticker);
                if (prevClose != null) {
                    currentPrice.setPrevClose(prevClose);
                }
                
                // 당일 데이터 초기화
                currentPrice.setOpen(currentPrice.getPrice());
                currentPrice.setHigh(currentPrice.getPrice());
                currentPrice.setLow(currentPrice.getPrice());
                currentPrice.setVolume(BigDecimal.ZERO);
                
                saveCurrentPrice(currentPrice);
                
                log.info("Initialized daily price: ticker={}, open={}, prevClose={}", 
                        ticker, currentPrice.getOpen(), currentPrice.getPrevClose());
            }
        } catch (Exception e) {
            log.error("Failed to initialize daily price: ticker={}", ticker, e);
        }
    }

    /**
     * Redis key 생성
     */
    private String buildRedisKey(String ticker) {
        return PRICE_KEY_PREFIX + ticker;
    }
}
