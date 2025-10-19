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
