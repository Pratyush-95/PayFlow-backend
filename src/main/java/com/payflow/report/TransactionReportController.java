
package com.payflow.report;

import com.payflow.service.MerchantService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/merchants")
public class TransactionReportController {

    private final TransactionReportService transactionReportService;
    private final MerchantService merchantService;

    public TransactionReportController(
            TransactionReportService transactionReportService,
            MerchantService merchantService) {

        this.transactionReportService = transactionReportService;
        this.merchantService = merchantService;
    }

    // Download transaction report as PDF
    @GetMapping("/{id}/reports/pdf")
    public ResponseEntity<byte[]> downloadTransactionReport(
            @PathVariable UUID id,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        // Verify that the merchant exists
        merchantService.getMerchantById(id);

        // Generate PDF report
        byte[] pdf = transactionReportService
                .generateTransactionReport(id, startDate, endDate);

        // Set PDF download headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData(
                "attachment",
                "payflow-transaction-report.pdf"
        );

        headers.setContentLength(pdf.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
