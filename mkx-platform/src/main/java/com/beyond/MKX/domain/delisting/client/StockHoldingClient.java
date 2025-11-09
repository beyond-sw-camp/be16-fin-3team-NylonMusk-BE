package com.beyond.MKX.domain.delisting.client;

import com.beyond.MKX.common.apiResponse.CommonDTO;
import com.beyond.MKX.domain.delisting.dto.StockHoldingResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * ordering 서비스의 주식 보유 정보를 조회하는 FeignClient
 * 상장폐지 보상금 계산 시 보유자 정보가 필요함
 */
@FeignClient(name = "ordering-service", contextId = "stockHoldingClient", url = "http://ordering-service")
public interface StockHoldingClient {

    /**
     * 특정 ticker의 모든 보유자 조회
     * 
     * @param ticker 주식 티커
     * @return CommonDTO로 감싸진 해당 주식을 보유한 모든 계좌 정보
     */
    @GetMapping("/api/internal/stock-holdings/ticker/{ticker}")
    CommonDTO<List<StockHoldingResDto>> getAllHoldersByTicker(@PathVariable("ticker") String ticker);

    /**
     * 특정 ticker의 모든 보유 정보 삭제 (상장폐지용)
     * 
     * @param ticker 주식 티커
     * @return CommonDTO로 감싸진 삭제된 개수
     */
    @DeleteMapping("/api/internal/stock-holdings/ticker/{ticker}")
    CommonDTO<Integer> deleteAllByTicker(@PathVariable("ticker") String ticker);
}
