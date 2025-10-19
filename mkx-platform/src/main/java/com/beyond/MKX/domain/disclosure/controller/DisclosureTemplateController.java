package com.beyond.MKX.domain.disclosure.controller;

import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/disclosures/templates")
public class DisclosureTemplateController {

    /** Earnings_Validation (단일 시트) 템플릿 다운로드 */
    @GetMapping(value = "/earnings-validation.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<ByteArrayResource> downloadSingleSheetTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Earnings_Validation");
            String[] headers = new String[] {
                    "fiscal_year", "fiscal_quarter", "revenue", "operating_income", "net_income",
                    "eps", "total_assets", "total_liabilities", "current_assets", "current_liabilities", "interest_expense"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = header.createCell(i); c.setCellValue(headers[i]);
                sheet.autoSizeColumn(i);
            }
            wb.write(bos);
            byte[] bytes = bos.toByteArray();
            ByteArrayResource res = new ByteArrayResource(bytes);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=Earnings_Validation.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(bytes.length)
                    .body(res);
        } catch (Exception e) {
            throw new IllegalStateException("템플릿 생성 실패", e);
        }
    }

    /** MKX 2시트 템플릿 다운로드 (CompanyFinancials / CashFlowStatement) */
    @GetMapping(value = "/mkx-2sheets.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<ByteArrayResource> downloadTwoSheetsTemplate() {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            // CompanyFinancials
            Sheet cf = wb.createSheet("CompanyFinancials");
            String[] cfHeaders = new String[] {
                    "fiscalYear", "fiscalQuarter", "revenue", "operatingIncome", "netIncome",
                    "eps", "totalAssets", "totalLiabilities", "totalEquity",
                    "currentAssets", "currentLiabilities", "interestExpense"
            };
            Row cfHeader = cf.createRow(0);
            for (int i = 0; i < cfHeaders.length; i++) {
                Cell c = cfHeader.createCell(i); c.setCellValue(cfHeaders[i]);
                cf.autoSizeColumn(i);
            }

            // CashFlowStatement
            Sheet cfs = wb.createSheet("CashFlowStatement");
            String[] cfsHeaders = new String[] {
                    "fiscalYear", "fiscalQuarter", "operatingCashFlow", "investingCashFlow", "financingCashFlow", "freeCashFlow"
            };
            Row cfsHeader = cfs.createRow(0);
            for (int i = 0; i < cfsHeaders.length; i++) {
                Cell c = cfsHeader.createCell(i); c.setCellValue(cfsHeaders[i]);
                cfs.autoSizeColumn(i);
            }

            wb.write(bos);
            byte[] bytes = bos.toByteArray();
            ByteArrayResource res = new ByteArrayResource(bytes);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=MKX_2sheets_template.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(bytes.length)
                    .body(res);
        } catch (Exception e) {
            throw new IllegalStateException("템플릿 생성 실패", e);
        }
    }
}
