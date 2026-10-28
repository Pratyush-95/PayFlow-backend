
package com.payflow.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Element;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfWriter;

import com.payflow.entity.PaymentOrder;
import com.payflow.repository.PaymentOrderRepository;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransactionReportService {

    private final PaymentOrderRepository paymentOrderRepository;

    public TransactionReportService(
            PaymentOrderRepository paymentOrderRepository) {
        this.paymentOrderRepository = paymentOrderRepository;
    }

    public byte[] generateTransactionReport(
            UUID merchantId,
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "Both startDate and endDate are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "startDate cannot be after endDate");
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        List<PaymentOrder> orders =
                paymentOrderRepository
                        .findByMerchant_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                                merchantId,
                                startDateTime,
                                endDateTime
                        );

        // Calculate financial summary for the selected date range
        long totalTransactions = orders.size();

        long successfulTransactions = orders.stream()
                .filter(order -> "SUCCESS".equalsIgnoreCase(order.getStatus()))
                .count();

        long failedTransactions = orders.stream()
                .filter(order -> "FAILED".equalsIgnoreCase(order.getStatus()))
                .count();

        long createdTransactions = orders.stream()
                .filter(order -> "CREATED".equalsIgnoreCase(order.getStatus()))
                .count();

        BigDecimal totalTransactionAmount = orders.stream()
                .map(PaymentOrder::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal successfulRevenue = orders.stream()
                .filter(order -> "SUCCESS".equalsIgnoreCase(order.getStatus()))
                .map(PaymentOrder::getAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageTransactionAmount = BigDecimal.ZERO;

        BigDecimal successRate = BigDecimal.ZERO;
        BigDecimal failureRate = BigDecimal.ZERO;

        if (totalTransactions > 0) {
            BigDecimal count = BigDecimal.valueOf(totalTransactions);

            averageTransactionAmount = totalTransactionAmount.divide(
                    count, 2, RoundingMode.HALF_UP);

            successRate = BigDecimal.valueOf(successfulTransactions)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(count, 2, RoundingMode.HALF_UP);

            failureRate = BigDecimal.valueOf(failedTransactions)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(count, 2, RoundingMode.HALF_UP);
        }

        try (ByteArrayOutputStream outputStream =
                     new ByteArrayOutputStream()) {

            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 18);

            Font sectionFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12);

            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 9);

            Font idFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 7);

            Font cellFont = FontFactory.getFont(
                    FontFactory.HELVETICA, 8);

            // Report header
            document.add(new Paragraph(
                    "PayFlow - Transaction Report", titleFont));

            String displayMerchantId =
                    shortDisplayId("Mrc_", merchantId, 15);

            document.add(new Paragraph(
                    "Merchant ID: " + displayMerchantId, cellFont));

            document.add(new Paragraph(
                    "Start Date: " + startDate, cellFont));

            document.add(new Paragraph(
                    "End Date: " + endDate, cellFont));

            document.add(new Paragraph(" "));

            // Financial summary section
            document.add(new Paragraph(
                    "Financial Summary", sectionFont));

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(75);
            summaryTable.setHorizontalAlignment(Element.ALIGN_LEFT);

            addSummaryRow(summaryTable, "Total Transactions",
                    String.valueOf(totalTransactions), headingFont, cellFont);

            addSummaryRow(summaryTable, "Successful Transactions",
                    String.valueOf(successfulTransactions), headingFont, cellFont);

            addSummaryRow(summaryTable, "Failed Transactions",
                    String.valueOf(failedTransactions), headingFont, cellFont);

            addSummaryRow(summaryTable, "Created Transactions",
                    String.valueOf(createdTransactions), headingFont, cellFont);

            addSummaryRow(summaryTable, "Total Transaction Amount",
                    totalTransactionAmount.toPlainString(), headingFont, cellFont);

            addSummaryRow(summaryTable, "Successful Revenue",
                    successfulRevenue.toPlainString(), headingFont, cellFont);

            addSummaryRow(summaryTable, "Average Transaction Amount",
                    averageTransactionAmount.toPlainString(), headingFont, cellFont);

            addSummaryRow(summaryTable, "Success Rate",
                    successRate.toPlainString() + "%", headingFont, cellFont);

            addSummaryRow(summaryTable, "Failure Rate",
                    failureRate.toPlainString() + "%", headingFont, cellFont);

            document.add(summaryTable);
            document.add(new Paragraph(" "));

            // Transaction details section
            document.add(new Paragraph(
                    "Transaction Details", sectionFont));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{3.5f, 2.5f, 1.2f, 1.2f, 1.5f});

            addHeader(table, "Transaction ID", headingFont);
            addHeader(table, "Created At", headingFont);
            addHeader(table, "Amount", headingFont);
            addHeader(table, "Currency", headingFont);
            addHeader(table, "Status", headingFont);

            for (PaymentOrder order : orders) {

                String displayTransactionId =
                        shortDisplayId("TXN_", order.getId(), 20);

                addCell(table, displayTransactionId, idFont);
                addCell(table, String.valueOf(order.getCreatedAt()), cellFont);
                addCell(table, String.valueOf(order.getAmount()), cellFont);
                addCell(table, String.valueOf(order.getCurrency()), cellFont);
                addCell(table, String.valueOf(order.getStatus()), cellFont);
            }

            document.add(table);
            document.close();

            return outputStream.toByteArray();

        } catch (DocumentException e) {
            throw new IllegalStateException(
                    "Failed to generate transaction PDF", e);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(
                    "Failed to create transaction PDF", e);
        }
    }

    // Format a short display ID
    private String shortDisplayId(
            String prefix,
            UUID id,
            int maxLength) {

        String value = prefix + id.toString().replace("-", "");

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    // Add a summary row
    private void addSummaryRow(
            PdfPTable table,
            String label,
            String value,
            Font labelFont,
            Font valueFont) {

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setPadding(5);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setPadding(5);
        table.addCell(valueCell);
    }

    // Add a table header
    private void addHeader(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(6);
        table.addCell(cell);
    }

    // Add a table cell
    private void addCell(
            PdfPTable table,
            String text,
            Font font) {

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(4);
        table.addCell(cell);
    }
}
