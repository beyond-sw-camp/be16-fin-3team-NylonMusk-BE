package com.beyond.MKX.domain.execution.repository;

import com.beyond.MKX.domain.execution.entity.Execution;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 체결 데이터 InfluxDB Repository
 * 
 * InfluxDB에 체결 데이터를 저장하고 조회하는 Repository
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExecutionInfluxRepository {

    private final InfluxDBClient influxDBClient;

    @Value("${influx.bucket}")
    private String bucket;

    @Value("${influx.org}")
    private String orgnaization;

    /**
     * 체결 데이터를 InfluxDB에 저장
     */
    public void save(Execution execution) {
        try {
            WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
            
            Point point = Point.measurement(Execution.MEASUREMENT)
                    .addTag("ticker", execution.getTicker())
                    .addTag("side", execution.getSide())
                    .addTag("execId", execution.getExecId())
                    .addField("marketOrderId", execution.getMarketOrderId())
                    .addField("counterOrderId", execution.getCounterOrderId())
                    .addField("price", execution.getPrice())
                    .addField("quantity", execution.getQuantity().doubleValue())
                    .time(execution.getTimestamp(), WritePrecision.MS);

            writeApi.writePoint(bucket, orgnaization, point);
            
            log.info("Execution saved to InfluxDB - execId: {}, ticker: {}, price: {}, quantity: {}", 
                    execution.getExecId(), execution.getTicker(), execution.getPrice(), execution.getQuantity());
        } catch (Exception e) {
            log.error("Failed to save execution to InfluxDB", e);
            throw new RuntimeException("Failed to save execution to InfluxDB", e);
        }
    }

    /**
     * 특정 기간의 체결 데이터 조회
     * 
     * @param ticker 종목 코드
     * @param start 시작 시각
     * @param end 종료 시각
     * @return 체결 데이터 리스트
     */
    public List<Execution> findExecutions(String ticker, Instant start, Instant end) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: %s, stop: %s) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                "|> sort(columns: [\"_time\"], desc: false)",
                bucket, start.toString(), end.toString(), Execution.MEASUREMENT, ticker
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, orgnaization);
            List<Execution> executions = new ArrayList<>();

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Execution execution = Execution.builder()
                            .ticker(ticker)
                            .side((String) record.getValueByKey("side"))
                            .execId((String) record.getValueByKey("execId"))
                            .marketOrderId((String) record.getValueByKey("marketOrderId"))
                            .counterOrderId((String) record.getValueByKey("counterOrderId"))
                            .price(((Number) record.getValueByKey("price")).longValue())
                            .quantity(BigDecimal.valueOf(((Number) record.getValueByKey("quantity")).doubleValue()))
                            .timestamp(record.getTime())
                            .build();
                    executions.add(execution);
                }
            }

            log.info("Retrieved {} executions for ticker: {} (period: {} ~ {})", 
                    executions.size(), ticker, start, end);
            return executions;
        } catch (Exception e) {
            log.error("Failed to retrieve executions from InfluxDB for period {} ~ {}", start, end, e);
            return new ArrayList<>();
        }
    }

    /**
     * 특정 종목의 최근 체결 데이터 조회
     */
    public List<Execution> findRecentExecutions(String ticker, String duration) {
        String flux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -%s) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\") " +
                "|> sort(columns: [\"_time\"], desc: true)",
                bucket, duration, Execution.MEASUREMENT, ticker
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, orgnaization);
            List<Execution> executions = new ArrayList<>();

            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Execution execution = Execution.builder()
                            .ticker(ticker)
                            .side((String) record.getValueByKey("side"))
                            .execId((String) record.getValueByKey("execId"))
                            .marketOrderId((String) record.getValueByKey("marketOrderId"))
                            .counterOrderId((String) record.getValueByKey("counterOrderId"))
                            .price(((Number) record.getValueByKey("price")).longValue())
                            .quantity(BigDecimal.valueOf(((Number) record.getValueByKey("quantity")).doubleValue()))
                            .timestamp(record.getTime())
                            .build();
                    executions.add(execution);
                }
            }

            log.info("Retrieved {} executions for ticker: {}", executions.size(), ticker);
            return executions;
        } catch (Exception e) {
            log.error("Failed to retrieve executions from InfluxDB", e);
            throw new RuntimeException("Failed to retrieve executions from InfluxDB", e);
        }
    }

    /**
     * 특정 기간의 OHLCV 데이터 조회 (캔들스틱 차트용)
     */
    public List<OHLCVData> getOHLCVData(String ticker, String interval, String duration) {
        // interval: 1m, 5m, 15m, 1h, 1d 등
        String flux = String.format(
                "from(bucket: \"%s\") " +
                "|> range(start: -%s) " +
                "|> filter(fn: (r) => r._measurement == \"%s\") " +
                "|> filter(fn: (r) => r.ticker == \"%s\") " +
                "|> filter(fn: (r) => r._field == \"price\" or r._field == \"quantity\") " +
                "|> aggregateWindow(every: %s, fn: last, createEmpty: false) " +
                "|> pivot(rowKey:[\"_time\"], columnKey: [\"_field\"], valueColumn: \"_value\")",
                bucket, duration, Execution.MEASUREMENT, ticker, interval
        );

        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux, orgnaization);
            List<OHLCVData> ohlcvList = new ArrayList<>();

            // OHLCV 데이터 계산을 위한 커스텀 로직
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    Instant time = record.getTime();
                    Long price = record.getValueByKey("price") != null ? 
                        ((Number) record.getValueByKey("price")).longValue() : null;
                    Double volume = record.getValueByKey("quantity") != null ? 
                        ((Number) record.getValueByKey("quantity")).doubleValue() : null;

                    if (price != null && volume != null) {
                        OHLCVData ohlcv = OHLCVData.builder()
                                .time(time)
                                .open(price)
                                .high(price)
                                .low(price)
                                .close(price)
                                .volume(BigDecimal.valueOf(volume))
                                .build();
                        ohlcvList.add(ohlcv);
                    }
                }
            }

            log.info("Retrieved {} OHLCV data points for ticker: {}", ohlcvList.size(), ticker);
            return ohlcvList;
        } catch (Exception e) {
            log.error("Failed to retrieve OHLCV data from InfluxDB", e);
            throw new RuntimeException("Failed to retrieve OHLCV data from InfluxDB", e);
        }
    }

    /**
     * OHLCV 데이터 내부 클래스
     */
    @Builder
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OHLCVData {
        private Instant time;
        private Long open;
        private Long high;
        private Long low;
        private Long close;
        private BigDecimal volume;
    }
}
