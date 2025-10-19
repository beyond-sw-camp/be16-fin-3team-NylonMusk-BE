package com.beyond.MKX.domain.financial.service;

import com.beyond.MKX.domain.financial.dto.CashFlowStatementReqDto;
import com.beyond.MKX.domain.financial.dto.CompanyFinancialsReqDto;
import com.beyond.MKX.domain.financial.dto.DisclosureEarningsValidationDto;
import com.beyond.MKX.domain.financial.dto.FinancialBundleReqDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialFileParser {

    private static final String SHEET_CF = "CompanyFinancials";
    private static final String SHEET_CFS = "CashFlowStatement";
    private static final String SHEET_DISCLOSURE = "Earnings_Validation";

    private static final List<String> REQUIRED_CF_HEADERS = List.of(
            "revenue", "operatingIncome", "netIncome", "totalAssets", "totalLiabilities"
    );

    /**
     * MKX 2시트 템플릿(CompanyFinancials / CashFlowStatement) 구조 검사
     */
    public boolean validateStructure(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet cf = wb.getSheet(SHEET_CF);
            Sheet cfs = wb.getSheet(SHEET_CFS);
            if (cf == null || cfs == null) {
                return false;
            }
            Row header = cf.getRow(0);
            if (header == null) return false;
            Set<String> names = headerNames(header);
            for (String h : REQUIRED_CF_HEADERS) {
                if (!names.contains(h)) return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Excel validateStructure 실패", e);
            return false;
        }
    }

    /**
     * 단일 시트 템플릿(Earnings_Validation) 구조 검사
     */
    public boolean validateEarningsStructure(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheet(SHEET_DISCLOSURE);
            if (sheet == null) return false;
            Row header = sheet.getRow(0);
            if (header == null) return false;

            Set<String> names = headerNames(header);
            List<String> required = List.of(
                    "disclosure_number", "ticker", "fiscal_year", "fiscal_quarter",
                    "revenue", "operating_income", "net_income", "eps",
                    "total_assets", "total_liabilities"
            );
            for (String h : required) {
                if (!names.contains(h)) {
                    log.warn("누락된 컬럼: {}", h);
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.error("Earnings 엑셀 구조 검증 실패", e);
            return false;
        }
    }

    /**
     * MKX 2시트 템플릿을 FinancialBundleReqDto로 파싱
     */
    public FinancialBundleReqDto parse(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            List<CompanyFinancialsReqDto> cfList = parseCompanyFinancials(wb.getSheet(SHEET_CF));
            List<CashFlowStatementReqDto> cfsList = parseCashFlow(wb.getSheet(SHEET_CFS));
            return new FinancialBundleReqDto(cfList, cfsList, null);
        } catch (Exception e) {
            throw new IllegalArgumentException("엑셀 파싱 실패: " + e.getMessage(), e);
        }
    }

    private List<CompanyFinancialsReqDto> parseCompanyFinancials(Sheet sheet) {
        if (sheet == null) return List.of();
        Row header = sheet.getRow(0);
        Map<String, Integer> col = headerIndex(header);
        required(col, "fiscalYear");
        // fiscalQuarter는 옵션
        required(col, "revenue");
        required(col, "operatingIncome");
        required(col, "netIncome");
        required(col, "totalAssets");
        required(col, "totalLiabilities");

        List<CompanyFinancialsReqDto> list = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            Integer fiscalQuarter = getInt(r, col.get("fiscalQuarter"));
            CompanyFinancialsReqDto dto = new CompanyFinancialsReqDto(
                    null, // stockId는 후단 서비스에서 주입
                    getIntRequired(r, col.get("fiscalYear")),
                    fiscalQuarter,
                    getLong(r, col.get("revenue")),
                    getLong(r, col.get("operatingIncome")),
                    getLong(r, col.get("netIncome")),
                    getDecimal(r, col.get("eps")),
                    getLong(r, col.get("totalAssets")),
                    getLong(r, col.get("totalLiabilities")),
                    getLong(r, col.get("totalEquity")),
                    getLong(r, col.get("currentAssets")),
                    getLong(r, col.get("currentLiabilities")),
                    getLong(r, col.get("interestExpense"))
            );
            list.add(dto);
        }
        return list;
    }

    private List<CashFlowStatementReqDto> parseCashFlow(Sheet sheet) {
        if (sheet == null) return List.of();
        Row header = sheet.getRow(0);
        Map<String, Integer> col = headerIndex(header);
        required(col, "fiscalYear");
        // fiscalQuarter 옵션
        required(col, "operatingCashFlow");
        required(col, "investingCashFlow");
        required(col, "financingCashFlow");
        required(col, "freeCashFlow");

        List<CashFlowStatementReqDto> list = new ArrayList<>();
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row r = sheet.getRow(i);
            if (r == null) continue;
            Integer fiscalQuarter = getInt(r, col.get("fiscalQuarter"));
            list.add(new CashFlowStatementReqDto(
                    null, // stockId 후단 주입
                    getIntRequired(r, col.get("fiscalYear")),
                    fiscalQuarter,
                    getLong(r, col.get("operatingCashFlow")),
                    getLong(r, col.get("investingCashFlow")),
                    getLong(r, col.get("financingCashFlow")),
                    getLong(r, col.get("freeCashFlow"))
            ));
        }
        return list;
    }

    /**
     * 헤더(1행) → 컬럼명:인덱스 맵 생성
     */
    private static Map<String, Integer> headerIndex(Row header) {
        if (header == null) throw new IllegalArgumentException("헤더가 없습니다.");
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.getLastCellNum(); i++) {
            Cell cell = header.getCell(i);
            if (cell == null) continue;
            String name = cell.getStringCellValue();
            if (name != null) map.put(name.trim(), i);
        }
        return map;
    }

    /** 헤더 컬렉션(Set) */
    private static Set<String> headerNames(Row header) {
        return new HashSet<>(headerIndex(header).keySet());
    }

    private static void required(Map<String, Integer> cols, String key) {
        if (!cols.containsKey(key)) {
            throw new IllegalArgumentException("필수 컬럼 누락: " + key);
        }
    }

    /**
     * 단일 시트(Earnings_Validation) 파싱 → 간단 DTO 리스트
     */
    public List<DisclosureEarningsValidationDto> parseEarningsValidation(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = WorkbookFactory.create(is)) {
            Sheet sheet = wb.getSheet(SHEET_DISCLOSURE);
            if (sheet == null) throw new IllegalArgumentException("시트가 없습니다: " + SHEET_DISCLOSURE);

            Row header = sheet.getRow(0);
            Map<String, Integer> col = headerIndex(header);

            List<DisclosureEarningsValidationDto> list = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row r = sheet.getRow(i);
                if (r == null) continue;
                DisclosureEarningsValidationDto dto = DisclosureEarningsValidationDto.builder()
                        .fiscalYear(getIntRequired(r, col.get("fiscal_year")))
                        .fiscalQuarter(getInt(r, col.get("fiscal_quarter")))
                        .revenue(getLong(r, col.get("revenue")))
                        .operatingIncome(getLong(r, col.get("operating_income")))
                        .netIncome(getLong(r, col.get("net_income")))
                        .eps(getDecimal(r, col.get("eps")))
                        .totalAssets(getLong(r, col.get("total_assets")))
                        .totalLiabilities(getLong(r, col.get("total_liabilities")))
                        .currentAssets(getLong(r, col.get("current_assets")))
                        .currentLiabilities(getLong(r, col.get("current_liabilities")))
                        .interestExpense(getLong(r, col.get("interest_expense")))
                        .build();
                list.add(dto);
            }
            return list;
        } catch (Exception e) {
            throw new IllegalArgumentException("Earnings 엑셀 파싱 실패: " + e.getMessage(), e);
        }
    }

    /** 셀 값을 문자열로 안전 추출 */
    private static final DataFormatter DF = new DataFormatter();

    private static Integer getInt(Row r, Integer idx) {
        String s = getString(r, idx);
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(cleanNumber(s)); } catch (Exception e) { return null; }
    }
    private static int getIntRequired(Row r, Integer idx) {
        Integer v = getInt(r, idx);
        if (v == null) throw new IllegalArgumentException("필수 정수 값 누락");
        return v;
    }
    private static Long getLong(Row r, Integer idx) {
        String s = getString(r, idx);
        if (s == null || s.isBlank()) return null;
        try {
            // BigDecimal 사용으로 과학표기(예: 4.5E11)도 허용
            java.math.BigDecimal bd = new java.math.BigDecimal(cleanNumber(s));
            return bd.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
        } catch (Exception e) {
            try { return Long.parseLong(cleanNumber(s)); } catch (Exception ignore) { return null; }
        }
    }
    private static BigDecimal getDecimal(Row r, Integer idx) {
        String s = getString(r, idx);
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(cleanNumber(s)); } catch (Exception e) { return null; }
    }

    private static String getString(Row r, Integer idx) {
        if (idx == null) return null;
        try { return DF.formatCellValue(r.getCell(idx)).trim(); } catch (Exception e) { return null; }
    }

    private static String cleanNumber(String s) {
        // 허용: 콤마, NBSP, 공백 제거
        return s.replaceAll("[,_\\u00A0\\s]", "");
    }
}
