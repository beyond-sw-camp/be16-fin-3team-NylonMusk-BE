package com.beyond.MKX.domain.financial.service;

import com.beyond.MKX.common.s3.S3Manager;
import com.beyond.MKX.domain.disclosure.entity.Disclosure;
import com.beyond.MKX.domain.disclosure.exception.InvalidDisclosureFileException;
import com.beyond.MKX.domain.financial.dto.*;
import com.beyond.MKX.domain.disclosure.service.EarningsDisclosureMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancialUploadService {

    private final S3Manager s3Manager;
    private final FinancialFileParser parser;
    private final FinancialAggregateService aggregateService;
    private final EarningsDisclosureMapper earningsDisclosureMapper;

    /** 파일 구조 검증 */
    public void validateFile(Disclosure disclosure) {
        MultipartFile file = downloadAsMultipart(disclosure);
        boolean ok = parser.validateEarningsStructure(file) || parser.validateStructure(file);
        if (!ok) {
            try {
                // DART 한글 헤더 파싱 폴백
                java.util.List<DisclosureEarningsValidationDto> parsed = earningsDisclosureMapper.parse(file, disclosure.getTickerSnapshot());
                ok = parsed != null && !parsed.isEmpty();
            } catch (Exception e) {
                ok = false;
            }
        }
        if (!ok) {
            log.error("[EARNINGS 검증 실패] disclosureId={} url={}", disclosure.getId(), disclosure.getFileUrl());
            throw new InvalidDisclosureFileException("엑셀 구조가 유효하지 않습니다. 필수 시트/컬럼을 확인하세요.");
        }
    }

    /** 검증 통과 후 파싱/저장 */
    @Transactional
    public void uploadFromDisclosure(Disclosure disclosure) {
        // 파일명/확장자로 엑셀 여부 판단(엑셀만 파싱, 그 외는 승인만 진행)
        String fileName = extractFilename(disclosure.getFileUrl());
        String lower = fileName == null ? "" : fileName.toLowerCase();
        boolean isExcel = lower.endsWith(".xlsx") || lower.endsWith(".xls");
        MultipartFile file = downloadAsMultipart(disclosure);

        if (!isExcel) {
            log.info("[EARNINGS 파싱 스킵] 엑셀이 아님: disclosureId={} filename={}", disclosure.getId(), fileName);
            return; // 소프트-패일: 데이터 저장 없이 승인만 진행
        }
        // 1) Earnings_Validation 템플릿이면 검증만 수행하고 저장은 생략
        if (parser.validateEarningsStructure(file)) {
            try {
                List<DisclosureEarningsValidationDto> rows = parser.parseEarningsValidation(file);
                log.info("Earnings_Validation rows parsed: {}", rows.size());

                // Earnings 검증 시트 → CompanyFinancials 저장으로 매핑 (CashFlow는 없음)
                List<CompanyFinancialsReqDto> cfList = rows.stream().map(r -> new CompanyFinancialsReqDto(
                        null,
                        r.getFiscalYear(),
                        r.getFiscalQuarter(),
                        r.getRevenue(),
                        r.getOperatingIncome(),
                        r.getNetIncome(),
                        r.getEps(),
                        r.getTotalAssets(),
                        r.getTotalLiabilities(),
                        null,
                        r.getCurrentAssets(),
                        r.getCurrentLiabilities(),
                        r.getInterestExpense()
                )).toList();

                FinancialBundleReqDto bundle = new FinancialBundleReqDto(cfList, List.of(), null);
                bundle = normalizeWithStockId(bundle, disclosure.getStockId());
                aggregateService.saveBundle(bundle);
            } catch (IllegalArgumentException e) {
                log.error("[EARNINGS 검증시트 파싱 실패] disclosureId={} error={}", disclosure.getId(), e.getMessage(), e);
                throw new InvalidDisclosureFileException("Earnings 검증 엑셀 파싱 실패: " + e.getMessage(), e);
            }
            return;
        }

        // 2) 일반 2시트 템플릿이면 파싱 후 저장
        if (parser.validateStructure(file)) {
            FinancialBundleReqDto bundle;
            try {
                bundle = parser.parse(file);
            } catch (IllegalArgumentException e) {
                log.error("[EARNINGS 파싱 실패] disclosureId={} error={}", disclosure.getId(), e.getMessage(), e);
                throw new InvalidDisclosureFileException("엑셀 파싱 실패: " + e.getMessage(), e);
            }

            UUID stockId = disclosure.getStockId();
            bundle = normalizeWithStockId(bundle, stockId);
            aggregateService.saveBundle(bundle);
            return;
        }

        // 3) DART 한글 헤더(원본) 폴백: EarningsDisclosureMapper 사용
        try {
            List<DisclosureEarningsValidationDto> rows = earningsDisclosureMapper.parse(file, disclosure.getTickerSnapshot());
            if (rows != null && !rows.isEmpty()) {
                List<CompanyFinancialsReqDto> cfList = rows.stream().map(r -> new CompanyFinancialsReqDto(
                        null,
                        r.getFiscalYear(),
                        r.getFiscalQuarter(),
                        r.getRevenue(),
                        r.getOperatingIncome(),
                        r.getNetIncome(),
                        r.getEps(),
                        r.getTotalAssets(),
                        r.getTotalLiabilities(),
                        null,
                        r.getCurrentAssets(),
                        r.getCurrentLiabilities(),
                        r.getInterestExpense()
                )).toList();
                FinancialBundleReqDto bundle = new FinancialBundleReqDto(cfList, java.util.List.of(), null);
                bundle = normalizeWithStockId(bundle, disclosure.getStockId());
                aggregateService.saveBundle(bundle);
                return;
            }
        } catch (IllegalArgumentException e) {
            log.error("[DART 파싱 실패] disclosureId={} error={}", disclosure.getId(), e.getMessage(), e);
            // fall-through to error below
        }

        // 4) 단일 시트(Earnings_Validation) 파싱 시도(시트명 무시, 헤더 기준)
        try {
            List<DisclosureEarningsValidationDto> rows = parser.parseEarningsValidation(file);
            if (rows != null && !rows.isEmpty()) {
                List<CompanyFinancialsReqDto> cfList = rows.stream().map(r -> new CompanyFinancialsReqDto(
                        null,
                        r.getFiscalYear(),
                        r.getFiscalQuarter(),
                        r.getRevenue(),
                        r.getOperatingIncome(),
                        r.getNetIncome(),
                        r.getEps(),
                        r.getTotalAssets(),
                        r.getTotalLiabilities(),
                        null,
                        r.getCurrentAssets(),
                        r.getCurrentLiabilities(),
                        r.getInterestExpense()
                )).toList();
                FinancialBundleReqDto bundle = new FinancialBundleReqDto(cfList, java.util.List.of(), null);
                bundle = normalizeWithStockId(bundle, disclosure.getStockId());
                aggregateService.saveBundle(bundle);
                return;
            }
        } catch (IllegalArgumentException e) {
            log.error("[EARNINGS_단일시트 파싱 실패] disclosureId={} error={}", disclosure.getId(), e.getMessage(), e);
        }

        // 엑셀 파일이지만 어떤 템플릿에도 맞지 않는 경우: 소프트-패일(승인만 진행)
        log.warn("[EARNINGS 파싱 스킵] 템플릿 미일치: disclosureId={} filename={}", disclosure.getId(), fileName);
        return;
    }

    private MultipartFile downloadAsMultipart(Disclosure disclosure) {
        String url = disclosure.getFileUrl();
        if (url == null || url.isBlank()) {
            throw new InvalidDisclosureFileException("첨부 파일 URL이 비어 있습니다.");
        }
        byte[] bytes = s3Manager.download(url);
        String filename = extractFilename(url);
        String contentType = guessContentType(filename);
        return new SimpleBytesMultipartFile(bytes, contentType, filename == null ? "file" : filename);
    }

    private String extractFilename(String url) {
        if (url == null) return null;
        int idx = url.lastIndexOf('/');
        String raw = (idx >= 0 && idx + 1 < url.length()) ? url.substring(idx + 1) : url;
        try { return java.net.URLDecoder.decode(raw, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception ignored) { return raw; }
    }

    private String guessContentType(String filename) {
        if (filename == null) return "application/octet-stream";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    private FinancialBundleReqDto normalizeWithStockId(FinancialBundleReqDto in, UUID stockId) {
        List<CompanyFinancialsReqDto> cf = in.companyFinancials() == null ? Collections.emptyList() : in.companyFinancials().stream()
                .map(d -> new CompanyFinancialsReqDto(stockId,
                        d.fiscalYear(), d.fiscalQuarter(), d.revenue(), d.operatingIncome(), d.netIncome(), d.eps(),
                        d.totalAssets(), d.totalLiabilities(), d.totalEquity(), d.currentAssets(), d.currentLiabilities(), d.interestExpense()))
                .toList();
        List<CashFlowStatementReqDto> cfs = in.cashFlowStatements() == null ? Collections.emptyList() : in.cashFlowStatements().stream()
                .map(d -> new CashFlowStatementReqDto(stockId,
                        d.fiscalYear(), d.fiscalQuarter(), d.operatingCashFlow(), d.investingCashFlow(), d.financingCashFlow(), d.freeCashFlow()))
                .toList();
        return new FinancialBundleReqDto(cf, cfs, in.financialRatios());
    }

    /**
     * IPO 상장 시 제출된 재무제표 파일을 "연간" 기준으로만 저장
     * - 분기 데이터는 생성하지 않음
     * - 연간 표식은 fiscalQuarter=4 로 강제 저장하여 고유키 충돌/조회 일관성 확보
     */
    @Transactional
    public void uploadAnnualFromUrl(UUID stockId, String fileUrl) {
        if (stockId == null || fileUrl == null || fileUrl.isBlank()) {
            log.warn("[IPO-ANNUAL-UPLOAD] 입력값 누락: stockId={}, fileUrl={}", stockId, fileUrl);
            return;
        }
        try {
            byte[] bytes = s3Manager.download(fileUrl);
            String filename = extractFilename(fileUrl);
            String contentType = guessContentType(filename);
            MultipartFile file = new SimpleBytesMultipartFile(bytes, contentType, filename == null ? "file" : filename);

            FinancialBundleReqDto bundle = null;
            // Earnings 검증 시트 우선 시도 → CompanyFinancials만 생성
            if (parser.validateEarningsStructure(file)) {
                var rows = parser.parseEarningsValidation(file);
                var cfList = rows.stream().map(r -> new CompanyFinancialsReqDto(
                        stockId,
                        r.getFiscalYear(),
                        4, // annual
                        r.getRevenue(), r.getOperatingIncome(), r.getNetIncome(), r.getEps(),
                        r.getTotalAssets(), r.getTotalLiabilities(), null,
                        r.getCurrentAssets(), r.getCurrentLiabilities(), r.getInterestExpense()
                )).toList();
                bundle = new FinancialBundleReqDto(cfList, List.of(), null);
            } else if (parser.validateStructure(file)) {
                // 2시트 템플릿 → 파싱 후 모든 항목을 annual(분기=4)로 강제 세팅
                FinancialBundleReqDto parsed = parser.parse(file);
                var cfList = (parsed.companyFinancials() == null ? List.<CompanyFinancialsReqDto>of() : parsed.companyFinancials()).stream()
                        .map(d -> new CompanyFinancialsReqDto(stockId, d.fiscalYear(), 4, d.revenue(), d.operatingIncome(), d.netIncome(), d.eps(),
                                d.totalAssets(), d.totalLiabilities(), d.totalEquity(), d.currentAssets(), d.currentLiabilities(), d.interestExpense()))
                        .toList();
                var cfsList = (parsed.cashFlowStatements() == null ? List.<CashFlowStatementReqDto>of() : parsed.cashFlowStatements()).stream()
                        .map(d -> new CashFlowStatementReqDto(stockId, d.fiscalYear(), 4, d.operatingCashFlow(), d.investingCashFlow(), d.financingCashFlow(), d.freeCashFlow()))
                        .toList();
                bundle = new FinancialBundleReqDto(cfList, cfsList, parsed.financialRatios());
            } else {
                log.warn("[IPO-ANNUAL-UPLOAD] 템플릿 불일치로 파싱 스킵: filename={}", filename);
                return;
            }

            aggregateService.saveBundle(bundle);
            log.info("[IPO-ANNUAL-UPLOAD] 연간 재무제표 저장 완료: stockId={}, years(cf)={}, years(cfs)={}",
                    stockId,
                    bundle.companyFinancials() == null ? 0 : bundle.companyFinancials().size(),
                    bundle.cashFlowStatements() == null ? 0 : bundle.cashFlowStatements().size());
        } catch (Exception e) {
            log.error("[IPO-ANNUAL-UPLOAD] 업로드 실패: stockId={}, url={}, error={}", stockId, fileUrl, e.getMessage(), e);
        }
    }

    /**
     * IPO 상장용 업로드: "연간 5개년 + 직전 분기 1개" 저장
     * - 템플릿에는 연간(quarter=4) 다섯 줄과, 최신 분기(1~3 중 하나) 한 줄이 함께 존재한다고 가정
     * - PER/PBR 등 시세 필요 지표는 제외, 저장 시 자동 계산 비율은 기존 로직대로 반영
     */
    @Transactional
    public void uploadIpoFinancials(UUID stockId, String fileUrl) {
        if (stockId == null || fileUrl == null || fileUrl.isBlank()) {
            log.warn("[IPO-UPLOAD] 입력값 누락: stockId={}, fileUrl={}", stockId, fileUrl);
            return;
        }
        try {
            byte[] bytes = s3Manager.download(fileUrl);
            String filename = extractFilename(fileUrl);
            String contentType = guessContentType(filename);
            MultipartFile file = new SimpleBytesMultipartFile(bytes, contentType, filename == null ? "file" : filename);

            // 파싱 (2시트 템플릿 권장, Earnings 검증 시트도 허용)
            FinancialBundleReqDto parsed;
            if (parser.validateStructure(file)) {
                parsed = parser.parse(file);
            } else if (parser.validateEarningsStructure(file)) {
                var rows = parser.parseEarningsValidation(file);
                var cfList = rows.stream().map(r -> new CompanyFinancialsReqDto(
                        null,
                        r.getFiscalYear(), r.getFiscalQuarter(),
                        r.getRevenue(), r.getOperatingIncome(), r.getNetIncome(), r.getEps(),
                        r.getTotalAssets(), r.getTotalLiabilities(), null,
                        r.getCurrentAssets(), r.getCurrentLiabilities(), r.getInterestExpense()
                )).toList();
                parsed = new FinancialBundleReqDto(cfList, List.of(), null);
            } else {
                log.warn("[IPO-UPLOAD] 템플릿 불일치: filename={}", filename);
                return;
            }

            // 분리: 연간(quarter=4) 중 최신 5개 연도 + 최신 분기(quarter in 1..3) 1개
            List<CompanyFinancialsReqDto> allCf = parsed.companyFinancials() == null ? List.of() : parsed.companyFinancials();
            List<CashFlowStatementReqDto> allCfs = parsed.cashFlowStatements() == null ? List.of() : parsed.cashFlowStatements();

            // 연간 5개년
            var annualFive = allCf.stream()
                    .filter(d -> d.fiscalQuarter() != null && d.fiscalQuarter() == 4)
                    .sorted(java.util.Comparator.comparingInt(CompanyFinancialsReqDto::fiscalYear).reversed())
                    .limit(5)
                    .map(d -> new CompanyFinancialsReqDto(stockId, d.fiscalYear(), 4,
                            d.revenue(), d.operatingIncome(), d.netIncome(), d.eps(),
                            d.totalAssets(), d.totalLiabilities(), d.totalEquity(), d.currentAssets(), d.currentLiabilities(), d.interestExpense()))
                    .toList();

            var annualCfsFive = allCfs.stream()
                    .filter(d -> d.fiscalQuarter() != null && d.fiscalQuarter() == 4)
                    .sorted(java.util.Comparator.comparingInt(CashFlowStatementReqDto::fiscalYear).reversed())
                    .limit(5)
                    .map(d -> new CashFlowStatementReqDto(stockId, d.fiscalYear(), 4,
                            d.operatingCashFlow(), d.investingCashFlow(), d.financingCashFlow(), d.freeCashFlow()))
                    .toList();

            // 최신 분기 1개 (quarter in 1..3)
            var latestQuarterOpt = allCf.stream()
                    .filter(d -> d.fiscalQuarter() != null && d.fiscalQuarter() != 4)
                    .max((a, b) -> {
                        int c = Integer.compare(a.fiscalYear(), b.fiscalYear());
                        if (c != 0) return c;
                        return Integer.compare(a.fiscalQuarter(), b.fiscalQuarter());
                    });

            var latestCfsOpt = allCfs.stream()
                    .filter(d -> d.fiscalQuarter() != null && d.fiscalQuarter() != 4)
                    .max((a, b) -> {
                        int c = Integer.compare(a.fiscalYear(), b.fiscalYear());
                        if (c != 0) return c;
                        return Integer.compare(a.fiscalQuarter(), b.fiscalQuarter());
                    });

            List<CompanyFinancialsReqDto> finalCf = new java.util.ArrayList<>(annualFive);
            latestQuarterOpt.ifPresent(d -> finalCf.add(new CompanyFinancialsReqDto(stockId, d.fiscalYear(), d.fiscalQuarter(),
                    d.revenue(), d.operatingIncome(), d.netIncome(), d.eps(),
                    d.totalAssets(), d.totalLiabilities(), d.totalEquity(), d.currentAssets(), d.currentLiabilities(), d.interestExpense())));

            List<CashFlowStatementReqDto> finalCfs = new java.util.ArrayList<>(annualCfsFive);
            latestCfsOpt.ifPresent(d -> finalCfs.add(new CashFlowStatementReqDto(stockId, d.fiscalYear(), d.fiscalQuarter(),
                    d.operatingCashFlow(), d.investingCashFlow(), d.financingCashFlow(), d.freeCashFlow())));

            FinancialBundleReqDto bundle = new FinancialBundleReqDto(finalCf, finalCfs, parsed.financialRatios());
            aggregateService.saveBundle(bundle);
            log.info("[IPO-UPLOAD] 저장 완료: stockId={}, annual_cf={}, latest_q_cf_present={}, annual_cfs={}, latest_q_cfs_present={}",
                    stockId, annualFive.size(), latestQuarterOpt.isPresent(), annualCfsFive.size(), latestCfsOpt.isPresent());
        } catch (Exception e) {
            log.error("[IPO-UPLOAD] 업로드 실패: stockId={}, url={}, error={}", stockId, fileUrl, e.getMessage(), e);
        }
    }

    /** 간단한 바이트 기반 MultipartFile 구현 */
    private static class SimpleBytesMultipartFile implements MultipartFile {
        private final byte[] bytes;
        private final String contentType;
        private final String originalFilename;

        SimpleBytesMultipartFile(byte[] bytes, String contentType, String originalFilename) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.originalFilename = originalFilename;
        }
        @Override public String getName() { return originalFilename; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes == null || bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
        @Override public void transferTo(File dest) throws IOException { throw new UnsupportedOperationException(); }
    }
}
