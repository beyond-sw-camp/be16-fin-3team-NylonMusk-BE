package com.beyond.MKX.domain.indicator;

import com.beyond.MKX.domain.indicator.dto.IndicatorResultDTO;
import com.beyond.MKX.domain.indicator.enums.IndicatorType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * NaN 값의 JSON 직렬화 테스트
 * 
 * JacksonConfig에 등록된 DoubleNaNSerializer가 
 * 모든 Calculator의 NaN 값을 null로 변환하는지 검증
 */
@SpringBootTest
class NaNSerializationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("단일 값 지표 - NaN이 null로 직렬화되는지 테스트 (MA, EMA, RSI)")
    void testSingleValueIndicatorNaNSerialization() throws Exception {
        // Given: NaN 값을 포함한 지표 데이터
        Map<String, Double> nanValues = new HashMap<>();
        nanValues.put("ma", Double.NaN);
        
        IndicatorResultDTO.IndicatorDataPoint nanPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:00:00Z"))
                .values(nanValues)
                .build();

        Map<String, Double> validValues = Map.of("ma", 50000.0);
        IndicatorResultDTO.IndicatorDataPoint validPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:05:00Z"))
                .values(validValues)
                .build();

        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .ticker("AAPL")
                .interval("5m")
                .indicatorType(IndicatorType.MA)
                .params(Map.of("period", 20))
                .data(List.of(nanPoint, validPoint))
                .calculatedAt(Instant.now())
                .dataPointCount(2)
                .build();

        // When: JSON 직렬화
        String json = objectMapper.writeValueAsString(result);

        // Then: NaN이 null로 변환되었는지 확인
        assertThat(json).contains("\"ma\":null");
        assertThat(json).contains("\"ma\":50000.0");
        assertThat(json).doesNotContain("NaN");
        
        System.out.println("✅ Single Value Indicator JSON:");
        System.out.println(json);
    }

    @Test
    @DisplayName("복수 값 지표 - NaN이 null로 직렬화되는지 테스트 (MACD, Bollinger Bands)")
    void testMultiValueIndicatorNaNSerialization() throws Exception {
        // Given: 여러 값에 NaN이 포함된 지표 데이터
        Map<String, Double> nanValues = new HashMap<>();
        nanValues.put("macd", Double.NaN);
        nanValues.put("signal", Double.NaN);
        nanValues.put("histogram", Double.NaN);
        
        IndicatorResultDTO.IndicatorDataPoint nanPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:00:00Z"))
                .values(nanValues)
                .build();

        Map<String, Double> validValues = Map.of(
                "macd", 100.0,
                "signal", 80.0,
                "histogram", 20.0
        );
        IndicatorResultDTO.IndicatorDataPoint validPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:05:00Z"))
                .values(validValues)
                .build();

        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .ticker("AAPL")
                .interval("5m")
                .indicatorType(IndicatorType.MACD)
                .params(Map.of("fastPeriod", 12, "slowPeriod", 26, "signalPeriod", 9))
                .data(List.of(nanPoint, validPoint))
                .calculatedAt(Instant.now())
                .dataPointCount(2)
                .build();

        // When: JSON 직렬화
        String json = objectMapper.writeValueAsString(result);

        // Then: 모든 NaN이 null로 변환되었는지 확인
        assertThat(json).contains("\"macd\":null");
        assertThat(json).contains("\"signal\":null");
        assertThat(json).contains("\"histogram\":null");
        assertThat(json).contains("\"macd\":100.0");
        assertThat(json).contains("\"signal\":80.0");
        assertThat(json).contains("\"histogram\":20.0");
        assertThat(json).doesNotContain("NaN");
        
        System.out.println("✅ Multi Value Indicator JSON:");
        System.out.println(json);
    }

    @Test
    @DisplayName("Bollinger Bands - 3개 값 모두 NaN 처리 테스트")
    void testBollingerBandsNaNSerialization() throws Exception {
        // Given: Bollinger Bands의 upper, middle, lower 모두 NaN
        Map<String, Double> nanValues = new HashMap<>();
        nanValues.put("upper", Double.NaN);
        nanValues.put("middle", Double.NaN);
        nanValues.put("lower", Double.NaN);
        
        IndicatorResultDTO.IndicatorDataPoint nanPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:00:00Z"))
                .values(nanValues)
                .build();

        Map<String, Double> validValues = Map.of(
                "upper", 52000.0,
                "middle", 50000.0,
                "lower", 48000.0
        );
        IndicatorResultDTO.IndicatorDataPoint validPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:05:00Z"))
                .values(validValues)
                .build();

        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .ticker("AAPL")
                .interval("5m")
                .indicatorType(IndicatorType.BOLLINGER_BANDS)
                .params(Map.of("period", 20, "stdDev", 2.0))
                .data(List.of(nanPoint, validPoint))
                .calculatedAt(Instant.now())
                .dataPointCount(2)
                .build();

        // When: JSON 직렬화
        String json = objectMapper.writeValueAsString(result);

        // Then: 모든 NaN이 null로 변환되었는지 확인
        assertThat(json).contains("\"upper\":null");
        assertThat(json).contains("\"middle\":null");
        assertThat(json).contains("\"lower\":null");
        assertThat(json).doesNotContain("NaN");
        
        System.out.println("✅ Bollinger Bands JSON:");
        System.out.println(json);
    }

    @Test
    @DisplayName("Infinity 값도 null로 직렬화되는지 테스트")
    void testInfinitySerialization() throws Exception {
        // Given: Infinity 값
        Map<String, Double> infinityValues = new HashMap<>();
        infinityValues.put("value1", Double.POSITIVE_INFINITY);
        infinityValues.put("value2", Double.NEGATIVE_INFINITY);
        infinityValues.put("value3", 100.0);
        
        IndicatorResultDTO.IndicatorDataPoint point = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.now())
                .values(infinityValues)
                .build();

        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .ticker("TEST")
                .interval("1m")
                .indicatorType(IndicatorType.RSI)
                .params(Map.of("period", 14))
                .data(List.of(point))
                .calculatedAt(Instant.now())
                .dataPointCount(1)
                .build();

        // When: JSON 직렬화
        String json = objectMapper.writeValueAsString(result);

        // Then: Infinity도 null로 변환
        assertThat(json).contains("\"value1\":null");
        assertThat(json).contains("\"value2\":null");
        assertThat(json).contains("\"value3\":100.0");
        assertThat(json).doesNotContain("Infinity");
        
        System.out.println("✅ Infinity Handling JSON:");
        System.out.println(json);
    }

    @Test
    @DisplayName("역직렬화 시 null이 NaN으로 변환되지 않는지 테스트")
    void testDeserialization() throws Exception {
        // Given: null 값을 포함한 JSON
        String json = """
                {
                    "ticker": "AAPL",
                    "interval": "5m",
                    "indicatorType": "MA",
                    "params": {"period": 20},
                    "data": [
                        {
                            "time": "2024-01-01T00:00:00Z",
                            "values": {"ma": null}
                        },
                        {
                            "time": "2024-01-01T00:05:00Z",
                            "values": {"ma": 50000.0}
                        }
                    ],
                    "calculatedAt": "2024-01-01T00:00:00Z",
                    "dataPointCount": 2
                }
                """;

        // When: JSON 역직렬화
        IndicatorResultDTO result = objectMapper.readValue(json, IndicatorResultDTO.class);

        // Then: null이 그대로 null로 유지되는지 확인
        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getValues().get("ma")).isNull();
        assertThat(result.getData().get(1).getValues().get("ma")).isEqualTo(50000.0);
        
        System.out.println("✅ Deserialization Result:");
        System.out.println("First point MA: " + result.getData().get(0).getValues().get("ma"));
        System.out.println("Second point MA: " + result.getData().get(1).getValues().get("ma"));
    }

    @Test
    @DisplayName("Stochastic - K와 D 값의 NaN 처리 테스트")
    void testStochasticNaNSerialization() throws Exception {
        // Given: Stochastic의 K, D 모두 NaN
        Map<String, Double> nanValues = new HashMap<>();
        nanValues.put("k", Double.NaN);
        nanValues.put("d", Double.NaN);
        
        IndicatorResultDTO.IndicatorDataPoint nanPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:00:00Z"))
                .values(nanValues)
                .build();

        Map<String, Double> partialNanValues = new HashMap<>();
        partialNanValues.put("k", 75.5);
        partialNanValues.put("d", Double.NaN);
        
        IndicatorResultDTO.IndicatorDataPoint partialNanPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:05:00Z"))
                .values(partialNanValues)
                .build();

        Map<String, Double> validValues = Map.of("k", 75.5, "d", 70.2);
        IndicatorResultDTO.IndicatorDataPoint validPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:10:00Z"))
                .values(validValues)
                .build();

        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .ticker("AAPL")
                .interval("5m")
                .indicatorType(IndicatorType.STOCHASTIC)
                .params(Map.of("kPeriod", 14, "dPeriod", 3, "smooth", 3))
                .data(List.of(nanPoint, partialNanPoint, validPoint))
                .calculatedAt(Instant.now())
                .dataPointCount(3)
                .build();

        // When: JSON 직렬화
        String json = objectMapper.writeValueAsString(result);

        // Then: NaN이 null로 변환되고, 일부만 NaN인 경우도 정상 처리
        assertThat(json).doesNotContain("NaN");
        
        System.out.println("✅ Stochastic JSON:");
        System.out.println(json);
    }

    @Test
    @DisplayName("ATR - TR과 ATR 값의 NaN 처리 테스트")
    void testATRNaNSerialization() throws Exception {
        // Given: ATR과 TR 값
        Map<String, Double> nanValues = new HashMap<>();
        nanValues.put("atr", Double.NaN);
        nanValues.put("tr", Double.NaN);
        
        IndicatorResultDTO.IndicatorDataPoint nanPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:00:00Z"))
                .values(nanValues)
                .build();

        Map<String, Double> validValues = Map.of("atr", 2.5, "tr", 3.0);
        IndicatorResultDTO.IndicatorDataPoint validPoint = IndicatorResultDTO.IndicatorDataPoint.builder()
                .time(Instant.parse("2024-01-01T00:05:00Z"))
                .values(validValues)
                .build();

        IndicatorResultDTO result = IndicatorResultDTO.builder()
                .ticker("AAPL")
                .interval("5m")
                .indicatorType(IndicatorType.ATR)
                .params(Map.of("period", 14))
                .data(List.of(nanPoint, validPoint))
                .calculatedAt(Instant.now())
                .dataPointCount(2)
                .build();

        // When: JSON 직렬화
        String json = objectMapper.writeValueAsString(result);

        // Then: NaN이 null로 변환
        assertThat(json).contains("\"atr\":null");
        assertThat(json).contains("\"tr\":null");
        assertThat(json).doesNotContain("NaN");
        
        System.out.println("✅ ATR JSON:");
        System.out.println(json);
    }
}
