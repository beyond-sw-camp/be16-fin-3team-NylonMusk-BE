package com.beyond.MKX.domain.chart.repository;

import com.beyond.MKX.domain.chart.entity.Candle;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 캔들 데이터 InfluxDB Repository
 * 
 * 확정된 캔들 데이터를 InfluxDB에 저장하고 조회
 * MKX_CandleData 버킷 사용 (영구 보관)
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CandleInfluxRepository {

    private final InfluxDBClient influxDBClient;

    @Value("${influx.candle-bucket}")
    private String candleBucket;

    @Value("${influx.org}")
    private String organization;

    // InfluxDB measurement name
    private static final String CANDLE_MEASUREMENT = "candles";

    /**
     * 확정된 캔들을 InfluxDB에 저장
     */
    public void save(Candle candle) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            
            Point point = Point.measurement(CANDLE_MEASUREMENT)
                    .addTag("ticker", candle.getTicker())
                    .addTag("interval", candle.getInterval())
                    .addField("open", candle.getOpen())
                    .addField("high", candle.getHigh())
                    .addField("low", candle.getLow())
                    .addField("close", candle.getClose())
                    .addField("volume", candle.getVolume() != null ? candle.getVolume().doubleValue() : 0.0)
                    .time(candle.getTime(), WritePrecision.MS);

            writeApi.writePoint(candleBucket, organization, point);
            
            log.debug("[CANDLE/INFLUX] Saved candle: ticker={}, interval={}, time={}, O={}, H={}, L={}, C={}, V={}", 
                    candle.getTicker(), candle.getInterval(), candle.getTime(),
                    candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume());
            
        } catch (Exception e) {
            log.error("[CANDLE/INFLUX] Failed to save candle: ticker={}, interval={}", 
                    candle.getTicker(), candle.getInterval(), e);
            throw new RuntimeException("Failed to save candle to InfluxDB", e);
        }
    }

    /**
     * 여러 캔들을 한 번에 저장 (Batch)
     */
    public void saveAll(List<Candle> candles) {
        try {
            if (candles == null || candles.isEmpty()) {
                return;
            }

            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            List<Point> points = new ArrayList<>();
            
            for (Candle candle : candles) {
                Point point = Point.measurement(CANDLE_MEASUREMENT)
                        .addTag("ticker", candle.getTicker())
                        .addTag("interval", candle.getInterval())
                        .addField("open", candle.getOpen())
                        .addField("high", candle.getHigh())
                        .addField("low", candle.getLow())
                        .addField("close", candle.getClose())
                        .addField("volume", candle.getVolume() != null ? candle.getVolume().doubleValue() : 0.0)
                        .time(candle.getTime(), WritePrecision.MS);
                points.add(point);
            }

            writeApi.writePoints(candleBucket, organization, points);
            
            log.info("[CANDLE/INFLUX] Batch saved {} candles", candles.size());
            
        } catch (Exception e) {
            log.error("[CANDLE/INFLUX] Failed to batch save candles", e);
            throw new RuntimeException("Failed to batch save candles to InfluxDB", e);
        }
    }

    /**
     * 특정 기간의 캔들 조회
     */
    public List<Candle> findCandles(String ticker, String interval, Instant start, Instant end) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: %s, stop: %s) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r.interval == \"%s\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                "|> sort(columns: [\"_time\"], desc: false)",
                candleBucket, 
                start.toString(), 
                end.toString(),
                CANDLE_MEASUREMENT, 
                ticker, 
                interval
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, organization);
            List<Candle> candles = new ArrayList<>();

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Candle candle = Candle.builder()
                            .ticker(ticker)
                            .interval(interval)
                            .time(record.getTime())
                            .open(((Number) record.getValueByKey("open")).longValue())
                            .high(((Number) record.getValueByKey("high")).longValue())
                            .low(((Number) record.getValueByKey("low")).longValue())
                            .close(((Number) record.getValueByKey("close")).longValue())
                            .volume(BigDecimal.valueOf(((Number) record.getValueByKey("volume")).doubleValue()))
                            .build();
                    candles.add(candle);
                }
            }

            log.debug("[CANDLE/INFLUX] Retrieved {} candles: ticker={}, interval={}, start={}, end={}", 
                    candles.size(), ticker, interval, start, end);
            
            return candles;
            
        } catch (Exception e) {
            log.error("[CANDLE/INFLUX] Failed to retrieve candles: ticker={}, interval={}", 
                    ticker, interval, e);
            return new ArrayList<>();
        }
    }

    /**
     * 특정 interval의 최신 캔들 조회
     */
    public Candle findLatestCandle(String ticker, String interval) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -7d) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r.interval == \"%s\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                "|> sort(columns: [\"_time\"], desc: true) " +
                "|> limit(n: 1)",
                candleBucket,
                CANDLE_MEASUREMENT,
                ticker,
                interval
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, organization);
            
            if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
                return null;
            }

            FluxRecord record = tables.get(0).getRecords().get(0);
            
            return Candle.builder()
                    .ticker(ticker)
                    .interval(interval)
                    .time(record.getTime())
                    .open(((Number) record.getValueByKey("open")).longValue())
                    .high(((Number) record.getValueByKey("high")).longValue())
                    .low(((Number) record.getValueByKey("low")).longValue())
                    .close(((Number) record.getValueByKey("close")).longValue())
                    .volume(BigDecimal.valueOf(((Number) record.getValueByKey("volume")).doubleValue()))
                    .build();
                    
        } catch (Exception e) {
            log.error("[CANDLE/INFLUX] Failed to retrieve latest candle: ticker={}, interval={}", 
                    ticker, interval, e);
            return null;
        }
    }

    /**
     * 특정 시간의 캔들 존재 여부 확인
     */
    public boolean exists(String ticker, String interval, Instant time) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: %s, stop: %s) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r.interval == \"%s\") " +
                "|> count()",
                candleBucket,
                time.toString(),
                time.plusSeconds(1).toString(),
                CANDLE_MEASUREMENT,
                ticker,
                interval
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, organization);
            
            if (tables.isEmpty() || tables.get(0).getRecords().isEmpty()) {
                return false;
            }

            long count = ((Number) tables.get(0).getRecords().get(0).getValue()).longValue();
            return count > 0;
            
        } catch (Exception e) {
            log.error("[CANDLE/INFLUX] Failed to check candle existence: ticker={}, interval={}, time={}", 
                    ticker, interval, time, e);
            return false;
        }
    }
}
