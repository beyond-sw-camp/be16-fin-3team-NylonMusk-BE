package com.beyond.MKX.common.config;

import com.beyond.MKX.domain.chart.websocket.ChartWebSocketHandler;
import com.beyond.MKX.domain.execution.websocket.ExecutionWebSocketHandler;
import com.beyond.MKX.domain.indicator.websocket.IndicatorWebSocketHandler;
import com.beyond.MKX.domain.orderbook.websocket.OrderBookWebSocketHandler;
import com.beyond.MKX.domain.price.websocket.CurrentPriceWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocket 설정
 * 
 * 실시간 차트, 호가, 현재가, 체결, 보조지표 데이터를 클라이언트에게 전송하기 위한 WebSocket 설정
 */
@Slf4j
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final ChartWebSocketHandler chartWebSocketHandler;
    private final OrderBookWebSocketHandler orderBookWebSocketHandler;
    private final CurrentPriceWebSocketHandler currentPriceWebSocketHandler;
    private final ExecutionWebSocketHandler executionWebSocketHandler;
    private final IndicatorWebSocketHandler indicatorWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("[WEBSOCKET] Registering WebSocket handlers...");
        
        // 차트 WebSocket 엔드포인트
        registry.addHandler(chartWebSocketHandler, "/ws/chart/{ticker}")
                .setAllowedOrigins("*")  // ✅ 프론트엔드 Origin 허용
                .setAllowedOriginPatterns("*");  // ✅ 패턴 허용

        // 호가 WebSocket 엔드포인트
        registry.addHandler(orderBookWebSocketHandler, "/ws/orderbook/{ticker}")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");

        // 현재가 WebSocket 엔드포인트
        registry.addHandler(currentPriceWebSocketHandler, "/ws/price/{ticker}")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
        
        // 체결 WebSocket 엔드포인트
        registry.addHandler(executionWebSocketHandler, "/ws/execution/{ticker}")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
        
        // 보조지표 WebSocket 엔드포인트
        registry.addHandler(indicatorWebSocketHandler, "/ws/indicator/{ticker}")
                .setAllowedOrigins("*")
                .setAllowedOriginPatterns("*");
        
        log.info("[WEBSOCKET] ✅ WebSocket handlers registered:");
        log.info("[WEBSOCKET]   - /ws/chart/{{ticker}}");
        log.info("[WEBSOCKET]   - /ws/orderbook/{{ticker}}");
        log.info("[WEBSOCKET]   - /ws/price/{{ticker}}");
        log.info("[WEBSOCKET]   - /ws/execution/{{ticker}}");
        log.info("[WEBSOCKET]   - /ws/indicator/{{ticker}}");
    }
}
