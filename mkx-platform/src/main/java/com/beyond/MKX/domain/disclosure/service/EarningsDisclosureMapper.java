package com.beyond.MKX.domain.disclosure.service;

import com.beyond.MKX.domain.financial.dto.DisclosureEarningsValidationDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Component
public class EarningsDisclosureMapper {

    private static final DataFormatter DF = new DataFormatter();

    /**
     * 공시 XLS(한글 헤더) → MKX 검증용 DTO 리스트로 변환
     */
    public List<DisclosureEarningsValidationDto> parse(MultipartFile file, String ticker) {
        List<DisclosureEarningsValidationDto> list = new ArrayList<>();
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) throw new IllegalArgumentException("첫 번째 행(헤더)이 없습니다.");

            Map<String, Integer> idx = detectKoreanHeaders(header);
            double unit = detectUnit(sheet);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;

                Integer fy = getInt(r, idx.get("fiscalYear"));
                Integer fq = getInt(r, idx.get("fiscalQuarter"));
                if (fy == null) continue; // 연도 없으면 스킵

                list.add(DisclosureEarningsValidationDto.builder()
                        .fiscalYear(fy)
                        .fiscalQuarter(fq)
                        .revenue(getLong(r, idx.get("revenue"), unit))
                        .operatingIncome(getLong(r, idx.get("operatingIncome"), unit))
                        .netIncome(getLong(r, idx.get("netIncome"), unit))
                        .eps(getDecimal(r, idx.get("eps")))
                        .totalAssets(getLong(r, idx.get("totalAssets"), unit))
                        .totalLiabilities(getLong(r, idx.get("totalLiabilities"), unit))
                        .currentAssets(getLong(r, idx.get("currentAssets"), unit))
                        .currentLiabilities(getLong(r, idx.get("currentLiabilities"), unit))
                        .interestExpense(getLong(r, idx.get("interestExpense"), unit))
                        .build());
            }
            return list;
        } catch (Exception e) {
            log.error("Earnings 공시 XLS 파싱 실패", e);
            throw new IllegalArgumentException("엑셀 파싱 오류: " + e.getMessage());
        }
    }

    /**
     * 한글 컬럼 감지 → 표준 키로 매핑 인덱스 생성
     */
    private Map<String, Integer> detectKoreanHeaders(Row header) {
        Map<String, Integer> raw = new HashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell c = CellUtil.getCell(header, i);
            String value = DF.formatCellValue(c).trim();
            if (!value.isBlank()) raw.put(value, i);
        }

        Map<String, Integer> map = new HashMap<>();
        // 연도/분기(필수: 연도)
        map.put("fiscalYear", firstIndex(raw, "회계연도", "연도", "년도", "fiscal_year", "year"));
        map.put("fiscalQuarter", firstIndex(raw, "분기", "회계분기", "fiscal_quarter", "quarter"));

        // 재무항목
        map.put("revenue", firstIndex(raw, "매출액", "매출", "매출수익", "수익", "revenue"));
        map.put("operatingIncome", firstIndex(raw, "영업이익", "operating_income"));
        map.put("netIncome", firstIndex(raw, "당기순이익", "순이익", "net_income"));
        map.put("eps", firstIndex(raw, "EPS", "주당순이익", "주당순이익(EPS)", "eps"));
        map.put("totalAssets", firstIndex(raw, "자산총계", "총자산", "total_assets"));
        map.put("totalLiabilities", firstIndex(raw, "부채총계", "총부채", "total_liabilities"));
        map.put("currentAssets", firstIndex(raw, "유동자산", "current_assets"));
        map.put("currentLiabilities", firstIndex(raw, "유동부채", "current_liabilities"));
        map.put("interestExpense", firstIndex(raw, "이자비용", "이자 비용", "interest_expense"));

        // 유효성 체크(최소 요건)
        if (map.get("fiscalYear") == null) {
            throw new IllegalArgumentException("필수 헤더 누락: 연도");
        }
        if (map.get("revenue") == null && map.get("operatingIncome") == null && map.get("netIncome") == null) {
            throw new IllegalArgumentException("필수 헤더 누락: 매출/영업이익/당기순이익 중 하나는 필요");
        }
        return map;
    }

    private Integer firstIndex(Map<String, Integer> raw, String... keys) {
        for (String k : keys) {
            for (Map.Entry<String, Integer> e : raw.entrySet()) {
                if (normalize(e.getKey()).contains(normalize(k))) return e.getValue();
            }
        }
        return null;
    }

    private String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[\u00A0\s]", "");
    }

    /** 단위 감지 (예: '단위: 백만원' → 1,000,000) */
    private double detectUnit(Sheet sheet) {
        try {
            for (int i = 0; i < Math.min(5, sheet.getLastRowNum() + 1); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                for (Cell c : r) {
                    String text = DF.formatCellValue(c);
                    if (text == null) continue;
                    String t = text.replaceAll("\s", "");
                    if (t.contains("단위")) {
                        if (t.contains("억")) return 100_000_000d;
                        if (t.contains("백만")) return 1_000_000d;
                        if (t.contains("천원")) return 1_000d;
                        if (t.contains("만원")) return 10_000d;
                        if (t.contains("원")) return 1d;
                    }
                }
            }
        } catch (Exception ignored) {}
        return 1.0d;
    }

    private Integer getInt(Row r, Integer idx) {
        String s = getString(r, idx);
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(cleanNumber(s)).setScale(0, RoundingMode.DOWN).intValue(); } catch (Exception e) { return null; }
    }

    private Long getLong(Row r, Integer idx, double unit) {
        String s = getString(r, idx);
        if (s == null || s.isBlank()) return null;
        try {
            BigDecimal bd = new BigDecimal(cleanNumber(s)).multiply(BigDecimal.valueOf(unit));
            return bd.setScale(0, RoundingMode.HALF_UP).longValue();
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal getDecimal(Row r, Integer idx) {
        String s = getString(r, idx);
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(cleanNumber(s)); } catch (Exception e) { return null; }
    }

    private String getString(Row r, Integer idx) {
        if (idx == null) return null;
        try { return DF.formatCellValue(r.getCell(idx)).trim(); } catch (Exception e) { return null; }
    }

    private String cleanNumber(String s) {
        String v = s.replaceAll("[,_\u00A0\s]", "");
        // 회계표기 (1,234) → -1234
        if (v.startsWith("(") && v.endsWith(")")) {
            v = "-" + v.substring(1, v.length() - 1);
        }
        return v;
    }
}
