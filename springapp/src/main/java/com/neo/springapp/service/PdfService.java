package com.neo.springapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.neo.springapp.model.TransferRecord;
import com.neo.springapp.model.Transaction;
import com.neo.springapp.model.Loan;
import com.neo.springapp.model.EmiPayment;
import com.neo.springapp.model.Cheque;
import com.neo.springapp.model.GoldLoan;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfService {

    public byte[] generateTransferReceipt(TransferRecord transfer) throws IOException {
        String html = generateTransferReceiptHtml(transfer);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    public byte[] generateBankStatement(String accountNumber, String userName, String userEmail, 
                                       Double currentBalance, List<Transaction> transactions) throws IOException {
        String html = generateBankStatementHtml(accountNumber, userName, userEmail, currentBalance, transactions);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generateTransferReceiptHtml(TransferRecord transfer) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedDate = transfer.getDate().format(formatter);
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>Transfer Receipt - NeoBank</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; position: relative; }")
            .append(".watermark { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 60px; color: rgba(30, 64, 175, 0.1); font-weight: bold; z-index: -1; pointer-events: none; white-space: nowrap; }")
            .append(".watermark-logo { position: fixed; top: 20%; left: 20%; transform: rotate(-30deg); font-size: 40px; color: rgba(30, 64, 175, 0.08); z-index: -1; pointer-events: none; }")
            .append(".receipt-container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); position: relative; z-index: 1; }")
            .append(".header { text-align: center; border-bottom: 3px solid #667eea; padding-bottom: 20px; margin-bottom: 30px; }")
            .append(".bank-logo { font-size: 24px; font-weight: bold; color: #667eea; margin-bottom: 10px; }")
            .append(".bank-name { font-size: 20px; color: #333; margin-bottom: 5px; }")
            .append(".bank-tagline { font-size: 14px; color: #666; font-style: italic; }")
            .append(".receipt-title { text-align: center; font-size: 18px; font-weight: bold; color: #333; margin-bottom: 30px; padding: 10px; background-color: #f8f9fa; border-radius: 5px; }")
            .append(".transfer-details { margin-bottom: 30px; }")
            .append(".detail-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #eee; }")
            .append(".detail-label { font-weight: bold; color: #555; width: 40%; }")
            .append(".detail-value { color: #333; width: 60%; text-align: right; }")
            .append(".amount-highlight { background-color: #e3f2fd; padding: 15px; border-radius: 8px; margin: 20px 0; text-align: center; }")
            .append(".amount-label { font-size: 14px; color: #666; margin-bottom: 5px; }")
            .append(".amount-value { font-size: 24px; font-weight: bold; color: #1976d2; }")
            .append(".status-badge { display: inline-block; padding: 5px 15px; border-radius: 20px; font-size: 12px; font-weight: bold; text-transform: uppercase; }")
            .append(".status-completed { background-color: #d4edda; color: #155724; }")
            .append(".status-cancelled { background-color: #f8d7da; color: #721c24; }")
            .append(".footer { margin-top: 30px; padding-top: 20px; border-top: 2px solid #eee; text-align: center; font-size: 12px; color: #666; }")
            .append(".bank-stamp { margin-top: 20px; text-align: center; font-size: 14px; color: #333; font-weight: bold; }")
            .append(".signature-line { border-top: 1px solid #333; width: 200px; margin: 20px auto; text-align: center; padding-top: 5px; font-size: 12px; color: #666; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"watermark\">NeoBank</div>")
            .append("<div class=\"watermark-logo\">🏦</div>")
            .append("<div class=\"receipt-container\">")
            .append("<div class=\"header\">")
            .append("<div class=\"bank-logo\">⭐ NeoBank</div>")
            .append("<div class=\"bank-name\">NeoBank</div>")
            .append("<div class=\"bank-tagline\">Relationship beyond banking</div>")
            .append("</div>")
            .append("<div class=\"receipt-title\">TRANSFER RECEIPT</div>")
            .append("<div class=\"transfer-details\">")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Transfer ID:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getTransferId()).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Date & Time:</div>")
            .append("<div class=\"detail-value\">").append(formattedDate).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Transfer Type:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getTransferType()).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Status:</div>")
            .append("<div class=\"detail-value\">")
            .append("<span class=\"status-badge status-").append(transfer.getStatus().toLowerCase()).append("\">").append(transfer.getStatus()).append("</span>")
            .append("</div>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"amount-highlight\">")
            .append("<div class=\"amount-label\">Transfer Amount</div>")
            .append("<div class=\"amount-value\">₹").append(String.format("%.2f", transfer.getAmount())).append("</div>")
            .append("</div>")
            .append("<div class=\"transfer-details\">")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Sender Name:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getSenderName()).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Sender Account:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getSenderAccountNumber()).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Recipient Name:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getRecipientName()).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Recipient Account:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getRecipientAccountNumber()).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">IFSC Code:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getIfsc()).append("</div>")
            .append("</div>")
            .append("<div class=\"detail-row\">")
            .append("<div class=\"detail-label\">Phone:</div>")
            .append("<div class=\"detail-value\">").append(transfer.getPhone()).append("</div>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"bank-stamp\">")
            .append("<div>BANK STAMP</div>")
            .append("<div class=\"signature-line\">Authorized Signature</div>")
            .append("</div>")
            .append("<div class=\"footer\">")
            .append("<p>This is a computer generated receipt and does not require signature.</p>")
            .append("<p>For any queries, contact us at: 1800 103 1906</p>")
            .append("<p>© 2025 NeoBank. All rights reserved.</p>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    private String generateBankStatementHtml(String accountNumber, String userName, String userEmail, 
                                            Double currentBalance, List<Transaction> transactions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String currentTime = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>Bank Statement - NeoBank</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; position: relative; }")
            .append(".watermark { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 60px; color: rgba(30, 64, 175, 0.1); font-weight: bold; z-index: -1; pointer-events: none; white-space: nowrap; }")
            .append(".watermark-logo { position: fixed; top: 20%; left: 20%; transform: rotate(-30deg); font-size: 40px; color: rgba(30, 64, 175, 0.08); z-index: -1; pointer-events: none; }")
            .append(".statement-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); position: relative; z-index: 1; }")
            .append(".header { text-align: center; border-bottom: 3px solid #667eea; padding-bottom: 20px; margin-bottom: 30px; }")
            .append(".bank-logo { font-size: 28px; font-weight: bold; color: #667eea; margin-bottom: 10px; }")
            .append(".bank-name { font-size: 22px; color: #333; margin-bottom: 5px; }")
            .append(".bank-tagline { font-size: 14px; color: #666; font-style: italic; }")
            .append(".statement-title { text-align: center; font-size: 20px; font-weight: bold; color: #333; margin-bottom: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }")
            .append(".account-info { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; }")
            .append(".info-item { margin-bottom: 10px; }")
            .append(".info-label { font-weight: bold; color: #555; font-size: 13px; }")
            .append(".info-value { color: #333; font-size: 14px; margin-top: 5px; }")
            .append(".balance-highlight { background-color: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center; border: 2px solid #1976d2; }")
            .append(".balance-label { font-size: 14px; color: #666; margin-bottom: 5px; }")
            .append(".balance-value { font-size: 28px; font-weight: bold; color: #1976d2; }")
            .append(".transactions-table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
            .append(".transactions-table th { background-color: #667eea; color: white; padding: 12px; text-align: left; font-weight: 600; font-size: 13px; }")
            .append(".transactions-table td { padding: 10px; border-bottom: 1px solid #eee; font-size: 12px; }")
            .append(".transactions-table tr:nth-child(even) { background-color: #f8f9fa; }")
            .append(".transactions-table tr:hover { background-color: #e3f2fd; }")
            .append(".credit { color: #28a745; font-weight: 600; }")
            .append(".debit { color: #dc3545; font-weight: 600; }")
            .append(".footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #eee; text-align: center; font-size: 12px; color: #666; }")
            .append(".summary { margin-top: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; }")
            .append(".summary-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #ddd; }")
            .append(".summary-label { font-weight: bold; color: #555; }")
            .append(".summary-value { color: #333; }")
            .append(".bank-seal { position: absolute; top: 20px; right: 20px; width: 100px; height: 100px; border: 3px solid #667eea; border-radius: 50%; background: white; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 10px; font-weight: bold; color: #667eea; text-align: center; padding: 10px; z-index: 10; }")
            .append("@media print { body { margin: 0; } .statement-container { box-shadow: none; } }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"watermark\">NeoBank</div>")
            .append("<div class=\"watermark-logo\">🏦</div>")
            .append("<div class=\"statement-container\">")
            .append("<div class=\"bank-seal\">")
            .append("<div>NEOBANK</div>")
            .append("<div style=\"font-size: 8px; margin-top: 3px;\">SEAL</div>")
            .append("<div style=\"font-size: 7px; margin-top: 2px;\">").append(currentDate).append("</div>")
            .append("</div>")
            .append("<div class=\"header\">")
            .append("<div class=\"bank-logo\">🏦 NeoBank</div>")
            .append("<div class=\"bank-name\">NeoBank India Limited</div>")
            .append("<div class=\"bank-tagline\">Relationship beyond banking</div>")
            .append("</div>")
            .append("<div class=\"statement-title\">ACCOUNT STATEMENT</div>")
            .append("<div class=\"account-info\">")
            .append("<div class=\"info-item\"><div class=\"info-label\">Account Number:</div><div class=\"info-value\">").append(accountNumber).append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Account Holder:</div><div class=\"info-value\">").append(userName != null ? userName : "N/A").append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Email:</div><div class=\"info-value\">").append(userEmail != null ? userEmail : "N/A").append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Statement Date:</div><div class=\"info-value\">").append(currentDate).append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Statement Time:</div><div class=\"info-value\">").append(currentTime).append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Total Transactions:</div><div class=\"info-value\">").append(transactions != null ? transactions.size() : 0).append("</div></div>")
            .append("</div>")
            .append("<div class=\"balance-highlight\">")
            .append("<div class=\"balance-label\">Current Balance</div>")
            .append("<div class=\"balance-value\">₹").append(String.format("%.2f", currentBalance != null ? currentBalance : 0.0)).append("</div>")
            .append("</div>");

        // Transactions table
        if (transactions != null && !transactions.isEmpty()) {
            html.append("<table class=\"transactions-table\">")
                .append("<thead>")
                .append("<tr>")
                .append("<th>Date & Time</th>")
                .append("<th>Description</th>")
                .append("<th>Type</th>")
                .append("<th>Amount (₹)</th>")
                .append("<th>Balance (₹)</th>")
                .append("</tr>")
                .append("</thead>")
                .append("<tbody>");

            for (Transaction txn : transactions) {
                String formattedDate = txn.getDate() != null ? txn.getDate().format(formatter) : "N/A";
                String typeClass = "Credit".equalsIgnoreCase(txn.getType()) ? "credit" : "debit";
                String amountPrefix = "Credit".equalsIgnoreCase(txn.getType()) ? "+" : "-";
                
                html.append("<tr>")
                    .append("<td>").append(formattedDate).append("</td>")
                    .append("<td>").append(txn.getDescription() != null ? txn.getDescription() : (txn.getMerchant() != null ? txn.getMerchant() : "N/A")).append("</td>")
                    .append("<td>").append(txn.getType() != null ? txn.getType() : "N/A").append("</td>")
                    .append("<td class=\"").append(typeClass).append("\">")
                    .append(amountPrefix).append("₹").append(String.format("%.2f", txn.getAmount() != null ? txn.getAmount() : 0.0))
                    .append("</td>")
                    .append("<td>₹").append(String.format("%.2f", txn.getBalance() != null ? txn.getBalance() : 0.0)).append("</td>")
                    .append("</tr>");
            }

            html.append("</tbody>")
                .append("</table>");
        } else {
            html.append("<div style=\"text-align: center; padding: 40px; color: #666;\">")
                .append("<p>No transactions found for this account.</p>")
                .append("</div>");
        }

        // Summary section
        if (transactions != null && !transactions.isEmpty()) {
            double totalCredit = transactions.stream()
                .filter(t -> "Credit".equalsIgnoreCase(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0)
                .sum();
            double totalDebit = transactions.stream()
                .filter(t -> "Debit".equalsIgnoreCase(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0)
                .sum();

            html.append("<div class=\"summary\">")
                .append("<h3 style=\"margin-top: 0; color: #333;\">Summary</h3>")
                .append("<div class=\"summary-row\">")
                .append("<span class=\"summary-label\">Total Credits:</span>")
                .append("<span class=\"summary-value credit\">+₹").append(String.format("%.2f", totalCredit)).append("</span>")
                .append("</div>")
                .append("<div class=\"summary-row\">")
                .append("<span class=\"summary-label\">Total Debits:</span>")
                .append("<span class=\"summary-value debit\">-₹").append(String.format("%.2f", totalDebit)).append("</span>")
                .append("</div>")
                .append("<div class=\"summary-row\" style=\"border-bottom: none; font-weight: bold; font-size: 14px;\">")
                .append("<span class=\"summary-label\">Net Amount:</span>")
                .append("<span class=\"summary-value\">₹").append(String.format("%.2f", totalCredit - totalDebit)).append("</span>")
                .append("</div>")
                .append("</div>");
        }

        html.append("<div class=\"footer\">")
            .append("<p><strong>This is a computer-generated statement and does not require signature.</strong></p>")
            .append("<p>For any queries, contact us at: 1800 103 1906 | support@neobank.in</p>")
            .append("<p>© ").append(java.time.Year.now()).append(" NeoBank. All rights reserved.</p>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    public byte[] generateForeclosureStatement(Loan loan, Map<String, Object> foreclosureDetails) throws IOException {
        String html = generateForeclosureStatementHtml(loan, foreclosureDetails);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generateForeclosureStatementHtml(Loan loan, Map<String, Object> foreclosureDetails) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String currentTime = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String foreclosureDate = loan.getForeclosureDate() != null ? 
            loan.getForeclosureDate().format(formatter) : currentDate + " " + currentTime;
        
        Double principalPaid = (Double) foreclosureDetails.get("principalPaid");
        Double interestPaid = (Double) foreclosureDetails.get("interestPaid");
        Double remainingPrincipal = (Double) foreclosureDetails.get("remainingPrincipal");
        Double remainingInterest = (Double) foreclosureDetails.get("remainingInterest");
        Double foreclosureCharges = (Double) foreclosureDetails.get("foreclosureCharges");
        Double gst = (Double) foreclosureDetails.get("gst");
        Double totalForeclosureAmount = (Double) foreclosureDetails.get("totalForeclosureAmount");
        Long monthsElapsed = (Long) foreclosureDetails.get("monthsElapsed");
        Integer remainingMonths = (Integer) foreclosureDetails.get("remainingMonths");
        Double emi = (Double) foreclosureDetails.get("emi");
        Double totalPaid = (Double) foreclosureDetails.get("totalPaid");
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>Loan Foreclosure Statement - NeoBank</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; position: relative; }")
            .append(".watermark { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 60px; color: rgba(30, 64, 175, 0.1); font-weight: bold; z-index: -1; pointer-events: none; white-space: nowrap; }")
            .append(".watermark-logo { position: fixed; top: 20%; left: 20%; transform: rotate(-30deg); font-size: 40px; color: rgba(30, 64, 175, 0.08); z-index: -1; pointer-events: none; }")
            .append(".statement-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); position: relative; z-index: 1; }")
            .append(".header { text-align: center; border-bottom: 3px solid #667eea; padding-bottom: 20px; margin-bottom: 30px; }")
            .append(".bank-logo { font-size: 28px; font-weight: bold; color: #667eea; margin-bottom: 10px; }")
            .append(".bank-name { font-size: 22px; color: #333; margin-bottom: 5px; }")
            .append(".bank-tagline { font-size: 14px; color: #666; font-style: italic; }")
            .append(".statement-title { text-align: center; font-size: 20px; font-weight: bold; color: #333; margin-bottom: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }")
            .append(".account-info { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 30px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; }")
            .append(".info-item { margin-bottom: 10px; }")
            .append(".info-label { font-weight: bold; color: #555; font-size: 13px; }")
            .append(".info-value { color: #333; font-size: 14px; margin-top: 5px; }")
            .append(".foreclosure-highlight { background-color: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; text-align: center; border: 2px solid #ffc107; }")
            .append(".foreclosure-label { font-size: 14px; color: #666; margin-bottom: 5px; }")
            .append(".foreclosure-value { font-size: 28px; font-weight: bold; color: #856404; }")
            .append(".calculation-table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
            .append(".calculation-table th { background-color: #667eea; color: white; padding: 12px; text-align: left; font-weight: 600; font-size: 13px; }")
            .append(".calculation-table td { padding: 10px; border-bottom: 1px solid #eee; font-size: 12px; }")
            .append(".calculation-table tr:nth-child(even) { background-color: #f8f9fa; }")
            .append(".calculation-table .amount { text-align: right; font-weight: 600; }")
            .append(".calculation-table .total-row { background-color: #e3f2fd; font-weight: bold; }")
            .append(".footer { margin-top: 40px; padding-top: 20px; border-top: 2px solid #eee; text-align: center; font-size: 12px; color: #666; }")
            .append("@media print { body { margin: 0; } .statement-container { box-shadow: none; } }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"watermark\">NeoBank</div>")
            .append("<div class=\"watermark-logo\">🏦</div>")
            .append("<div class=\"statement-container\">")
            .append("<div class=\"header\">")
            .append("<div class=\"bank-logo\">🏦 NeoBank</div>")
            .append("<div class=\"bank-name\">NeoBank India Limited</div>")
            .append("<div class=\"bank-tagline\">Relationship beyond banking</div>")
            .append("</div>")
            .append("<div class=\"statement-title\">LOAN FORECLOSURE STATEMENT</div>")
            .append("<div class=\"account-info\">")
            .append("<div class=\"info-item\"><div class=\"info-label\">Loan Account Number:</div><div class=\"info-value\">").append(loan.getLoanAccountNumber()).append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Customer Name:</div><div class=\"info-value\">").append(loan.getUserName() != null ? loan.getUserName() : "N/A").append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Account Number:</div><div class=\"info-value\">").append(loan.getAccountNumber()).append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Loan Type:</div><div class=\"info-value\">").append(loan.getType() != null ? loan.getType() : "N/A").append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Foreclosure Date:</div><div class=\"info-value\">").append(foreclosureDate).append("</div></div>")
            .append("<div class=\"info-item\"><div class=\"info-label\">Foreclosed By:</div><div class=\"info-value\">").append(loan.getForeclosedBy() != null ? loan.getForeclosedBy() : "Admin").append("</div></div>")
            .append("</div>")
            .append("<div class=\"foreclosure-highlight\">")
            .append("<div class=\"foreclosure-label\">Total Foreclosure Amount</div>")
            .append("<div class=\"foreclosure-value\">₹").append(String.format("%.2f", totalForeclosureAmount)).append("</div>")
            .append("</div>");

        // Loan details table
        html.append("<table class=\"calculation-table\">")
            .append("<thead>")
            .append("<tr>")
            .append("<th>Description</th>")
            .append("<th class=\"amount\">Amount (₹)</th>")
            .append("</tr>")
            .append("</thead>")
            .append("<tbody>")
            .append("<tr>")
            .append("<td>Original Loan Amount</td>")
            .append("<td class=\"amount\">₹").append(String.format("%.2f", loan.getAmount())).append("</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Loan Tenure</td>")
            .append("<td class=\"amount\">").append(loan.getTenure()).append(" months</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Interest Rate</td>")
            .append("<td class=\"amount\">").append(String.format("%.2f", loan.getInterestRate())).append("% p.a.</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Monthly EMI</td>")
            .append("<td class=\"amount\">₹").append(String.format("%.2f", emi)).append("</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Months Elapsed</td>")
            .append("<td class=\"amount\">").append(monthsElapsed).append(" months</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Remaining Months</td>")
            .append("<td class=\"amount\">").append(remainingMonths).append(" months</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Total Amount Paid (EMI × Months)</td>")
            .append("<td class=\"amount\">₹").append(String.format("%.2f", totalPaid)).append("</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Principal Paid</td>")
            .append("<td class=\"amount\">₹").append(String.format("%.2f", principalPaid)).append("</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Interest Paid</td>")
            .append("<td class=\"amount\">₹").append(String.format("%.2f", interestPaid)).append("</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td><strong>Remaining Principal</strong></td>")
            .append("<td class=\"amount\"><strong>₹").append(String.format("%.2f", remainingPrincipal)).append("</strong></td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td><strong>Remaining Interest</strong></td>")
            .append("<td class=\"amount\"><strong>₹").append(String.format("%.2f", remainingInterest)).append("</strong></td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>Foreclosure Charges (4% of Remaining Principal)</td>")
            .append("<td class=\"amount\">₹").append(String.format("%.2f", foreclosureCharges)).append("</td>")
            .append("</tr>")
            .append("<tr>")
            .append("<td>GST (18% on Foreclosure Charges)</td>")
            .append("<td class=\"amount\">₹").append(String.format("%.2f", gst)).append("</td>")
            .append("</tr>")
            .append("<tr class=\"total-row\">")
            .append("<td><strong>TOTAL FORECLOSURE AMOUNT</strong></td>")
            .append("<td class=\"amount\"><strong>₹").append(String.format("%.2f", totalForeclosureAmount)).append("</strong></td>")
            .append("</tr>")
            .append("</tbody>")
            .append("</table>");

        html.append("<div class=\"footer\">")
            .append("<p><strong>This is a computer-generated foreclosure statement and does not require signature.</strong></p>")
            .append("<p>For any queries, contact us at: 1800 103 1906 | support@neobank.in</p>")
            .append("<p>© ").append(java.time.Year.now()).append(" NeoBank. All rights reserved.</p>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    public byte[] generateEmiReceipt(EmiPayment emi, Loan loan) throws IOException {
        String html = generateEmiReceiptHtml(emi, loan);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generateEmiReceiptHtml(EmiPayment emi, Loan loan) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String currentDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String paymentDate = emi.getPaymentDate() != null ? 
            emi.getPaymentDate().format(formatter) : currentDate;
        String dueDate = emi.getDueDate() != null ? 
            emi.getDueDate().format(dateFormatter) : currentDate;
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>EMI Payment Receipt - NeoBank</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; position: relative; }")
            .append(".watermark { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 60px; color: rgba(30, 64, 175, 0.1); font-weight: bold; z-index: -1; pointer-events: none; white-space: nowrap; }")
            .append(".receipt-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); position: relative; z-index: 1; }")
            .append(".header { text-align: center; border-bottom: 3px solid #667eea; padding-bottom: 20px; margin-bottom: 30px; }")
            .append(".bank-logo { font-size: 28px; font-weight: bold; color: #667eea; margin-bottom: 10px; }")
            .append(".bank-name { font-size: 22px; color: #333; margin-bottom: 5px; }")
            .append(".receipt-title { text-align: center; font-size: 20px; font-weight: bold; color: #333; margin-bottom: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }")
            .append(".info-section { margin-bottom: 25px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; }")
            .append(".info-row { display: flex; justify-content: space-between; margin-bottom: 12px; padding: 8px 0; border-bottom: 1px solid #e0e0e0; }")
            .append(".info-row:last-child { border-bottom: none; }")
            .append(".info-label { font-weight: bold; color: #555; font-size: 14px; }")
            .append(".info-value { color: #333; font-size: 14px; text-align: right; }")
            .append(".amount-section { background-color: #e8f5e9; padding: 20px; border-radius: 8px; margin: 20px 0; }")
            .append(".amount-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 16px; }")
            .append(".amount-row.total { font-size: 20px; font-weight: bold; color: #2e7d32; border-top: 2px solid #4caf50; padding-top: 10px; margin-top: 10px; }")
            .append(".status-badge { display: inline-block; padding: 8px 16px; border-radius: 20px; font-weight: bold; font-size: 14px; }")
            .append(".status-paid { background-color: #4caf50; color: white; }")
            .append(".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; color: #666; font-size: 12px; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"watermark\">NeoBank</div>")
            .append("<div class=\"receipt-container\">")
            .append("<div class=\"header\">")
            .append("<div class=\"bank-logo\">🏦 NeoBank</div>")
            .append("<div class=\"bank-name\">EMI Payment Receipt</div>")
            .append("</div>")
            .append("<div class=\"receipt-title\">Payment Confirmation</div>")
            .append("<div class=\"info-section\">")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Receipt Number:</span>")
            .append("<span class=\"info-value\">EMI-").append(emi.getId()).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Payment Date:</span>")
            .append("<span class=\"info-value\">").append(paymentDate).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Due Date:</span>")
            .append("<span class=\"info-value\">").append(dueDate).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Status:</span>")
            .append("<span class=\"info-value\"><span class=\"status-badge status-paid\">PAID</span></span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"info-section\">")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Loan Account Number:</span>")
            .append("<span class=\"info-value\">").append(emi.getLoanAccountNumber()).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Account Number:</span>")
            .append("<span class=\"info-value\">").append(emi.getAccountNumber()).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Loan Type:</span>")
            .append("<span class=\"info-value\">").append(loan.getType()).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Customer Name:</span>")
            .append("<span class=\"info-value\">").append(loan.getUserName()).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">EMI Installment:</span>")
            .append("<span class=\"info-value\">#").append(emi.getEmiNumber()).append(" of ").append(loan.getTenure()).append("</span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"amount-section\">")
            .append("<div class=\"amount-row\">")
            .append("<span>Principal Amount:</span>")
            .append("<span>₹").append(String.format("%.2f", emi.getPrincipalAmount())).append("</span>")
            .append("</div>")
            .append("<div class=\"amount-row\">")
            .append("<span>Interest Amount:</span>")
            .append("<span>₹").append(String.format("%.2f", emi.getInterestAmount())).append("</span>")
            .append("</div>")
            .append("<div class=\"amount-row total\">")
            .append("<span>Total EMI Amount:</span>")
            .append("<span>₹").append(String.format("%.2f", emi.getTotalAmount())).append("</span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"info-section\">")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Remaining Principal:</span>")
            .append("<span class=\"info-value\">₹").append(String.format("%.2f", emi.getRemainingPrincipal())).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Balance Before Payment:</span>")
            .append("<span class=\"info-value\">₹").append(emi.getBalanceBeforePayment() != null ? String.format("%.2f", emi.getBalanceBeforePayment()) : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Balance After Payment:</span>")
            .append("<span class=\"info-value\">₹").append(emi.getBalanceAfterPayment() != null ? String.format("%.2f", emi.getBalanceAfterPayment()) : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Transaction ID:</span>")
            .append("<span class=\"info-value\">").append(emi.getTransactionId() != null ? emi.getTransactionId() : "N/A").append("</span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"footer\">")
            .append("<p><strong>This is a computer-generated receipt and does not require signature.</strong></p>")
            .append("<p>For any queries, contact us at: 1800 103 1906 | support@neobank.in</p>")
            .append("<p>© ").append(java.time.Year.now()).append(" NeoBank. All rights reserved.</p>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    public byte[] generateChequePdf(Cheque cheque) throws IOException {
        String html = generateChequeHtml(cheque);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generateChequeHtml(Cheque cheque) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String currentDate = java.time.LocalDateTime.now().format(formatter);
        
        // Parse date for date boxes
        String[] dateParts = currentDate.split("-");
        String day1 = dateParts[0].substring(0, 1);
        String day2 = dateParts[0].substring(1, 2);
        String month1 = dateParts[1].substring(0, 1);
        String month2 = dateParts[1].substring(1, 2);
        String year = dateParts[2];
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>Cheque - NeoBank</title>")
            .append("<style>")
            .append("@page { size: A4 landscape; margin: 0; }")
            .append("body { font-family: 'Arial', sans-serif; margin: 0; padding: 0; background: linear-gradient(135deg, #fff5e6 0%, #ffe8cc 100%); }")
            .append(".cheque-wrapper { width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; padding: 20px; box-sizing: border-box; }")
            .append(".cheque-container { width: 900px; min-height: 400px; background: white; position: relative; border: 2px solid #333; box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15); }")
            .append(".cheque-background { position: absolute; top: 0; left: 0; width: 100%; height: 100%; opacity: 0.03; background-image: radial-gradient(circle, #ff8c42 2px, transparent 2px); background-size: 40px 40px; pointer-events: none; }")
            .append(".cheque-content { position: relative; z-index: 1; padding: 30px 40px; }")
            .append(".cheque-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 25px; padding-bottom: 15px; border-bottom: 2px solid #333; }")
            .append(".bank-name-section { display: flex; align-items: center; gap: 10px; }")
            .append(".bank-caret { font-size: 24px; color: #333; font-weight: bold; }")
            .append(".bank-name { font-size: 22px; font-weight: bold; color: #000; letter-spacing: 1px; }")
            .append(".date-section { display: flex; gap: 8px; align-items: flex-end; }")
            .append(".date-box { width: 35px; height: 35px; border: 2px solid #333; display: inline-flex; align-items: center; justify-content: center; font-size: 16px; font-weight: bold; background: white; }")
            .append(".date-label { font-size: 10px; text-align: center; margin-top: 2px; color: #666; }")
            .append(".date-row { display: flex; flex-direction: column; align-items: center; gap: 2px; }")
            .append(".cheque-body { display: grid; grid-template-columns: 1fr 1fr; gap: 30px; margin: 25px 0; }")
            .append(".left-section { display: flex; flex-direction: column; gap: 20px; }")
            .append(".right-section { display: flex; flex-direction: column; gap: 20px; align-items: flex-end; }")
            .append(".field-group { display: flex; align-items: baseline; gap: 10px; }")
            .append(".field-label { font-size: 13px; font-weight: bold; color: #333; min-width: 80px; }")
            .append(".field-line { flex: 1; border-bottom: 2px solid #000; height: 25px; position: relative; }")
            .append(".field-line.small { max-width: 200px; }")
            .append(".field-line.medium { max-width: 300px; }")
            .append(".field-line.large { max-width: 400px; }")
            .append(".amount-words-section { margin: 20px 0; }")
            .append(".amount-words-label { font-size: 13px; font-weight: bold; color: #333; margin-bottom: 8px; }")
            .append(".amount-words-line { border-bottom: 2px solid #000; height: 25px; width: 100%; }")
            .append(".amount-figures-section { display: flex; gap: 8px; align-items: center; margin-top: 20px; }")
            .append(".amount-box { width: 50px; height: 50px; border: 2px solid #333; display: inline-flex; align-items: center; justify-content: center; font-size: 18px; font-weight: bold; background: white; }")
            .append(".rupee-symbol { font-size: 20px; font-weight: bold; margin-right: 5px; }")
            .append(".account-number-section { margin-top: 20px; }")
            .append(".account-box { width: 200px; height: 35px; border: 2px solid #333; display: inline-flex; align-items: center; justify-content: center; font-size: 14px; font-weight: bold; background: white; padding: 5px 10px; }")
            .append(".account-label { font-size: 11px; color: #666; margin-bottom: 5px; }")
            .append(".or-bearer { font-size: 12px; color: #666; margin-left: 10px; }")
            .append(".cheque-footer { display: flex; justify-content: space-between; align-items: flex-end; margin-top: 40px; padding-top: 20px; border-top: 1px solid #ccc; }")
            .append(".signature-section { display: flex; flex-direction: column; align-items: center; gap: 5px; }")
            .append(".signature-line { width: 250px; border-bottom: 2px solid #000; height: 30px; }")
            .append(".signature-label { font-size: 11px; color: #666; margin-top: 5px; }")
            .append(".micr-section { display: flex; gap: 15px; align-items: center; margin-top: 15px; padding: 10px 0; border-top: 1px solid #ccc; font-family: 'Courier New', monospace; font-size: 14px; font-weight: bold; letter-spacing: 2px; }")
            .append(".micr-code { padding: 5px 10px; background: #f5f5f5; border: 1px solid #ddd; }")
            .append(".cheque-number-display { position: absolute; top: 10px; right: 15px; font-size: 11px; color: #666; font-weight: normal; }")
            .append(".cancelled-mark { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 120px; color: rgba(220, 53, 69, 0.4); font-weight: bold; z-index: 10; pointer-events: none; letter-spacing: 10px; }")
            .append("@media print { body { background: white; } .cheque-wrapper { padding: 0; } .cheque-container { box-shadow: none; border: none; } }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"cheque-wrapper\">")
            .append("<div class=\"cheque-container\">")
            .append("<div class=\"cheque-background\"></div>");
        
        // Add cancelled mark if cheque is cancelled
        if ("CANCELLED".equals(cheque.getStatus())) {
            html.append("<div class=\"cancelled-mark\">CANCELLED</div>");
        }
        
        html.append("<div class=\"cheque-number-display\">Cheque No: ").append(cheque.getChequeNumber()).append("</div>")
            .append("<div class=\"cheque-content\">")
            
            // Header with Bank Name and Date
            .append("<div class=\"cheque-header\">")
            .append("<div class=\"bank-name-section\">")
            .append("<span class=\"bank-caret\">▲</span>")
            .append("<span class=\"bank-name\">NEOBANK</span>")
            .append("</div>")
            .append("<div class=\"date-section\">")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(day1).append("</div>")
            .append("<div class=\"date-label\">D</div>")
            .append("</div>")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(day2).append("</div>")
            .append("<div class=\"date-label\">D</div>")
            .append("</div>")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(month1).append("</div>")
            .append("<div class=\"date-label\">M</div>")
            .append("</div>")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(month2).append("</div>")
            .append("<div class=\"date-label\">M</div>")
            .append("</div>")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(year.substring(0, 1)).append("</div>")
            .append("<div class=\"date-label\">Y</div>")
            .append("</div>")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(year.substring(1, 2)).append("</div>")
            .append("<div class=\"date-label\">Y</div>")
            .append("</div>")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(year.substring(2, 3)).append("</div>")
            .append("<div class=\"date-label\">Y</div>")
            .append("</div>")
            .append("<div class=\"date-row\">")
            .append("<div class=\"date-box\">").append(year.substring(3, 4)).append("</div>")
            .append("<div class=\"date-label\">Y</div>")
            .append("</div>")
            .append("</div>")
            .append("</div>")
            
            // Cheque Body
            .append("<div class=\"cheque-body\">")
            .append("<div class=\"left-section\">")
            
            // PAY field
            .append("<div class=\"field-group\">")
            .append("<span class=\"field-label\">PAY</span>")
            .append("<div class=\"field-line large\"></div>")
            .append("</div>")
            
            // SUM OF field
            .append("<div class=\"amount-words-section\">")
            .append("<div class=\"amount-words-label\">SUM OF</div>")
            .append("<div class=\"amount-words-line\"></div>")
            .append("</div>")
            
            // Account Number
            .append("<div class=\"account-number-section\">")
            .append("<div class=\"account-label\">Acc. No.</div>")
            .append("<div class=\"account-box\">").append(cheque.getAccountNumber() != null ? cheque.getAccountNumber() : "").append("</div>")
            .append("</div>")
            
            .append("</div>")
            .append("<div class=\"right-section\">")
            
            // OR BEARER
            .append("<div class=\"field-group\">")
            .append("<div class=\"field-line small\"></div>")
            .append("<span class=\"or-bearer\">OR BEARER</span>")
            .append("</div>")
            
            // Amount in figures
            .append("<div class=\"amount-figures-section\">")
            .append("<span class=\"rupee-symbol\">₹</span>")
            .append("<div class=\"amount-box\"></div>")
            .append("<div class=\"amount-box\"></div>")
            .append("<div class=\"amount-box\"></div>")
            .append("<div class=\"amount-box\"></div>")
            .append("<div class=\"amount-box\"></div>")
            .append("<div class=\"amount-box\"></div>")
            .append("<div class=\"amount-box\"></div>")
            .append("<div class=\"amount-box\"></div>")
            .append("</div>")
            
            .append("</div>")
            .append("</div>")
            
            // Footer with Signature and MICR
            .append("<div class=\"cheque-footer\">")
            .append("<div class=\"signature-section\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Please Sign Above</div>")
            .append("</div>")
            .append("</div>")
            
            // MICR Codes
            .append("<div class=\"micr-section\">")
            .append("<span class=\"micr-code\">567890⑈</span>")
            .append("<span class=\"micr-code\">").append(cheque.getMicrCode() != null ? cheque.getMicrCode() : "1234567890").append("⑆</span>")
            .append("<span class=\"micr-code\">1234</span>")
            .append("</div>")
            
            .append("</div>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    public byte[] generatePassbook(Long userId, com.neo.springapp.model.User user, com.neo.springapp.model.Account account, Double currentBalance) throws IOException {
        String html = generatePassbookHtml(userId, user, account, currentBalance);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generatePassbookHtml(Long userId, com.neo.springapp.model.User user, com.neo.springapp.model.Account account, Double currentBalance) {
        String currentDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String accountOpenDate = user.getJoinDate() != null ? 
            user.getJoinDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : currentDate;
        
        // Base64 encode profile photo and signature
        String profilePhotoBase64 = "";
        String signatureBase64 = "";
        
        if (user.getProfilePhoto() != null && user.getProfilePhoto().length > 0) {
            profilePhotoBase64 = java.util.Base64.getEncoder().encodeToString(user.getProfilePhoto());
            String photoMimeType = user.getProfilePhotoType() != null ? user.getProfilePhotoType() : "image/jpeg";
            profilePhotoBase64 = "data:" + photoMimeType + ";base64," + profilePhotoBase64;
        }
        
        if (user.getSignature() != null && user.getSignature().length > 0) {
            signatureBase64 = java.util.Base64.getEncoder().encodeToString(user.getSignature());
            String sigMimeType = user.getSignatureType() != null ? user.getSignatureType() : "image/jpeg";
            signatureBase64 = "data:" + sigMimeType + ";base64," + signatureBase64;
        }
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>Passbook - NeoBank</title>")
            .append("<style>")
            .append("@page { size: A4; margin: 15mm; }")
            .append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; background: white; }")
            .append(".passbook-container { width: 100%; max-width: 210mm; margin: 0 auto; background: white; padding: 20px; position: relative; }")
            .append(".digital-banner { background: linear-gradient(135deg, #0077cc 0%, #005fa3 100%); color: white; padding: 15px; text-align: center; margin-bottom: 20px; border-radius: 5px; }")
            .append(".digital-banner h2 { margin: 0; font-size: 18px; font-weight: bold; }")
            .append(".digital-icons { display: flex; justify-content: center; gap: 30px; margin: 15px 0; }")
            .append(".digital-icon { text-align: center; }")
            .append(".icon-circle { width: 60px; height: 60px; border-radius: 50%; background: rgba(255,255,255,0.2); display: flex; align-items: center; justify-content: center; margin: 0 auto 8px; font-size: 24px; }")
            .append(".icon-label { font-size: 11px; color: rgba(255,255,255,0.9); }")
            .append(".digital-info { display: flex; gap: 15px; margin-top: 10px; }")
            .append(".info-box { flex: 1; background: rgba(255,255,255,0.15); padding: 8px; border-radius: 4px; font-size: 11px; }")
            .append(".bank-header { text-align: center; margin: 20px 0; border-bottom: 2px solid #0077cc; padding-bottom: 15px; }")
            .append(".bank-name { font-size: 22px; font-weight: bold; color: #0077cc; margin-bottom: 5px; }")
            .append(".account-details { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin: 20px 0; }")
            .append(".detail-column { }")
            .append(".detail-item { margin-bottom: 12px; display: flex; align-items: flex-start; }")
            .append(".detail-label { font-weight: bold; color: #333; font-size: 12px; min-width: 140px; }")
            .append(".detail-value { color: #333; font-size: 12px; flex: 1; }")
            .append(".photo-signature-section { display: grid; grid-template-columns: 200px 1fr; gap: 20px; margin: 20px 0; align-items: start; }")
            .append(".photo-container { position: relative; }")
            .append(".passport-photo { width: 200px; height: 250px; border: 3px solid #0077cc; border-radius: 4px; object-fit: cover; background: #f8f9fa; }")
            .append(".photo-placeholder { width: 200px; height: 250px; border: 3px solid #0077cc; border-radius: 4px; background: #f8f9fa; display: flex; align-items: center; justify-content: center; color: #666; font-size: 12px; }")
            .append(".bank-stamp { position: absolute; bottom: 10px; right: 10px; width: 80px; height: 80px; border: 2px solid #0077cc; border-radius: 50%; background: white; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 10px; font-weight: bold; color: #0077cc; text-align: center; padding: 5px; }")
            .append(".signature-container { margin-top: 20px; }")
            .append(".signature-label { font-weight: bold; font-size: 12px; margin-bottom: 8px; color: #333; }")
            .append(".signature-box { width: 100%; max-width: 300px; height: 80px; border: 2px solid #333; border-radius: 4px; background: white; display: flex; align-items: center; justify-content: center; }")
            .append(".signature-image { max-width: 100%; max-height: 100%; object-fit: contain; }")
            .append(".signature-placeholder { color: #999; font-size: 11px; }")
            .append(".footer-section { margin-top: 30px; padding-top: 15px; border-top: 1px solid #ddd; display: flex; justify-content: space-between; align-items: flex-end; }")
            .append(".account-ref { font-size: 11px; color: #666; }")
            .append(".ref-number { font-size: 11px; color: #333; font-weight: bold; }")
            .append(".authorized-sign { text-align: center; }")
            .append(".sign-line { width: 200px; border-top: 2px solid #333; margin: 0 auto 5px; }")
            .append(".sign-label { font-size: 11px; color: #666; }")
            .append("@media print { body { margin: 0; } .passbook-container { padding: 10px; } }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"passbook-container\">")
            
            // Digital Banking Banner
            .append("<div class=\"digital-banner\">")
            .append("<h2>Go digital.</h2>")
            .append("<div class=\"digital-icons\">")
            .append("<div class=\"digital-icon\">")
            .append("<div class=\"icon-circle\">📱</div>")
            .append("<div class=\"icon-label\">PAYZAPP</div>")
            .append("</div>")
            .append("<div class=\"digital-icon\">")
            .append("<div class=\"icon-circle\">💳</div>")
            .append("<div class=\"icon-label\">UPI</div>")
            .append("</div>")
            .append("<div class=\"digital-icon\">")
            .append("<div class=\"icon-circle\">📋</div>")
            .append("<div class=\"icon-label\">BillPay</div>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"digital-info\">")
            .append("<div class=\"info-box\">Cashless payments, anytime, anywhere</div>")
            .append("<div class=\"info-box\">Use your UPI ID to send and receive money 24x7</div>")
            .append("<div class=\"info-box\">Set and Forget Standing Instructions</div>")
            .append("</div>")
            .append("</div>")
            
            // Bank Header
            .append("<div class=\"bank-header\">")
            .append("<div class=\"bank-name\">NEOBANK LTD</div>")
            .append("</div>")
            
            // Account Details in Two Columns
            .append("<div class=\"account-details\">")
            .append("<div class=\"detail-column\">")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Account Number:</span><span class=\"detail-value\">").append(user.getAccountNumber() != null ? user.getAccountNumber() : "N/A").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Customer Name:</span><span class=\"detail-value\">").append(account != null && account.getName() != null ? account.getName() : user.getUsername()).append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Customer Id:</span><span class=\"detail-value\">").append(user.getId()).append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">A/C Open Date:</span><span class=\"detail-value\">").append(accountOpenDate).append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Joint Holder:</span><span class=\"detail-value\">None</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Address:</span><span class=\"detail-value\">").append(account != null && account.getAddress() != null ? account.getAddress() : "Not provided").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">City:</span><span class=\"detail-value\">").append("Not provided").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Pin Code:</span><span class=\"detail-value\">").append("N/A").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">State:</span><span class=\"detail-value\">").append("Not provided").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Country:</span><span class=\"detail-value\">INDIA</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Res Tel No:</span><span class=\"detail-value\">").append("").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Mobile No:</span><span class=\"detail-value\">").append(account != null && account.getPhone() != null ? account.getPhone() : "N/A").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Nomination:</span><span class=\"detail-value\">").append("Registered").append("</span></div>")
            .append("</div>")
            .append("<div class=\"detail-column\">")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Product Code:</span><span class=\"detail-value\">").append(account != null && account.getAccountType() != null ? account.getAccountType() : "Savings").append("</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">A/C Currency:</span><span class=\"detail-value\">INR</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Branch Code:</span><span class=\"detail-value\">NEO0008648</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Branch Name:</span><span class=\"detail-value\">NEOBANK HEAD OFFICE</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Branch Address:</span><span class=\"detail-value\">NEOBANK HEAD OFFICE, MUMBAI</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">City:</span><span class=\"detail-value\">MUMBAI</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Pin Code:</span><span class=\"detail-value\">400001</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">State:</span><span class=\"detail-value\">MAHARASHTRA</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Country:</span><span class=\"detail-value\">India</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Branch Tel No:</span><span class=\"detail-value\">1800 103 1906</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">MICR Code:</span><span class=\"detail-value\">400000001</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">IFSC Code:</span><span class=\"detail-value\">NEO0008648</span></div>")
            .append("<div class=\"detail-item\"><span class=\"detail-label\">Current Balance:</span><span class=\"detail-value\" style=\"font-weight: bold; color: #0077cc; font-size: 14px;\">₹").append(String.format("%.2f", currentBalance != null ? currentBalance : 0.0)).append("</span></div>")
            .append("</div>")
            .append("</div>")
            
            // Photo and Signature Section
            .append("<div class=\"photo-signature-section\">")
            .append("<div class=\"photo-container\">");
        
        if (!profilePhotoBase64.isEmpty()) {
            html.append("<img src=\"").append(profilePhotoBase64).append("\" alt=\"Profile Photo\" class=\"passport-photo\" />");
        } else {
            html.append("<div class=\"photo-placeholder\">No Photo</div>");
        }
        
        html.append("<div class=\"bank-stamp\">")
            .append("<div>NEOBANK</div>")
            .append("<div style=\"font-size: 8px; margin-top: 3px;\">").append(currentDate).append("</div>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"signature-container\">")
            .append("<div class=\"signature-label\">Customer Signature:</div>")
            .append("<div class=\"signature-box\">");
        
        if (!signatureBase64.isEmpty()) {
            html.append("<img src=\"").append(signatureBase64).append("\" alt=\"Signature\" class=\"signature-image\" />");
        } else {
            html.append("<div class=\"signature-placeholder\">Signature Not Available</div>");
        }
        
        html.append("</div>")
            .append("</div>")
            .append("</div>")
            
            // Footer
            .append("<div class=\"footer-section\">")
            .append("<div class=\"account-ref\">")
            .append("<div>Account Number: ").append(user.getAccountNumber() != null ? user.getAccountNumber() : "N/A").append("</div>")
            .append("<div>Ref. No: NEO-").append(String.format("%05d", userId)).append("-").append(currentDate.replace("-", "")).append("</div>")
            .append("</div>")
            .append("<div class=\"authorized-sign\">")
            .append("<div class=\"sign-line\"></div>")
            .append("<div class=\"sign-label\">Authorized Signatory</div>")
            .append("<div class=\"sign-label\" style=\"margin-top: 5px;\">NeoBank Ltd.</div>")
            .append("</div>")
            .append("</div>")
            
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    public byte[] generateGoldLoanReceipt(GoldLoan goldLoan) throws IOException {
        String html = generateGoldLoanReceiptHtml(goldLoan);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generateGoldLoanReceiptHtml(GoldLoan goldLoan) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String approvalDate = goldLoan.getApprovalDate() != null ? 
            goldLoan.getApprovalDate().format(formatter) : currentDate;
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>Gold Loan Receipt - NeoBank</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; position: relative; }")
            .append(".watermark { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 60px; color: rgba(30, 64, 175, 0.1); font-weight: bold; z-index: -1; pointer-events: none; white-space: nowrap; }")
            .append(".receipt-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); position: relative; z-index: 1; }")
            .append(".header { text-align: center; border-bottom: 3px solid #667eea; padding-bottom: 20px; margin-bottom: 30px; }")
            .append(".bank-logo { font-size: 28px; font-weight: bold; color: #667eea; margin-bottom: 10px; }")
            .append(".bank-name { font-size: 22px; color: #333; margin-bottom: 5px; }")
            .append(".bank-seal { position: absolute; top: 20px; right: 20px; width: 100px; height: 100px; border: 3px solid #667eea; border-radius: 50%; background: white; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 10px; font-weight: bold; color: #667eea; text-align: center; padding: 10px; }")
            .append(".receipt-title { text-align: center; font-size: 20px; font-weight: bold; color: #333; margin-bottom: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }")
            .append(".info-section { margin-bottom: 25px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; }")
            .append(".info-row { display: flex; justify-content: space-between; margin-bottom: 12px; padding: 8px 0; border-bottom: 1px solid #e0e0e0; }")
            .append(".info-row:last-child { border-bottom: none; }")
            .append(".info-label { font-weight: bold; color: #555; font-size: 14px; }")
            .append(".info-value { color: #333; font-size: 14px; text-align: right; }")
            .append(".amount-section { background-color: #fff3cd; padding: 20px; border-radius: 8px; margin: 20px 0; border: 2px solid #ffc107; }")
            .append(".amount-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 16px; }")
            .append(".amount-row.total { font-size: 20px; font-weight: bold; color: #856404; border-top: 2px solid #ffc107; padding-top: 10px; margin-top: 10px; }")
            .append(".gold-details { background-color: #e8f5e9; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #4caf50; }")
            .append(".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; color: #666; font-size: 12px; }")
            .append(".signature-section { display: flex; justify-content: space-between; margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; }")
            .append(".signature-box { text-align: center; flex: 1; }")
            .append(".signature-line { width: 200px; border-top: 2px solid #333; margin: 0 auto 5px; }")
            .append(".signature-label { font-size: 11px; color: #666; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"watermark\">NeoBank</div>")
            .append("<div class=\"receipt-container\">")
            .append("<div class=\"bank-seal\">")
            .append("<div>NEOBANK</div>")
            .append("<div style=\"font-size: 8px; margin-top: 3px;\">SEAL</div>")
            .append("<div style=\"font-size: 7px; margin-top: 2px;\">").append(currentDate).append("</div>")
            .append("</div>")
            .append("<div class=\"header\">")
            .append("<div class=\"bank-logo\">🏦 NeoBank</div>")
            .append("<div class=\"bank-name\">Gold Loan Receipt</div>")
            .append("</div>")
            .append("<div class=\"receipt-title\">Loan Approval Confirmation</div>")
            .append("<div class=\"info-section\">")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Receipt Number:</span>")
            .append("<span class=\"info-value\">GL-").append(goldLoan.getId()).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Loan Account Number:</span>")
            .append("<span class=\"info-value\">").append(goldLoan.getLoanAccountNumber() != null ? goldLoan.getLoanAccountNumber() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Approval Date:</span>")
            .append("<span class=\"info-value\">").append(approvalDate).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Status:</span>")
            .append("<span class=\"info-value\"><strong style=\"color: #4caf50;\">APPROVED</strong></span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Customer Name:</span>")
            .append("<span class=\"info-value\">").append(goldLoan.getUserName() != null ? goldLoan.getUserName() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Account Number:</span>")
            .append("<span class=\"info-value\">").append(goldLoan.getAccountNumber() != null ? goldLoan.getAccountNumber() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Email:</span>")
            .append("<span class=\"info-value\">").append(goldLoan.getUserEmail() != null ? goldLoan.getUserEmail() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Approved By:</span>")
            .append("<span class=\"info-value\">").append(goldLoan.getApprovedBy() != null ? goldLoan.getApprovedBy() : "Admin").append("</span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"gold-details\">")
            .append("<h3 style=\"margin-top: 0; color: #2e7d32;\">Gold Details</h3>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Gold Weight (Grams):</span>")
            .append("<span class=\"info-value\">").append(goldLoan.getGoldGrams() != null ? String.format("%.2f", goldLoan.getGoldGrams()) : "N/A").append(" grams</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Gold Rate (per gram):</span>")
            .append("<span class=\"info-value\">₹").append(goldLoan.getGoldRatePerGram() != null ? String.format("%.2f", goldLoan.getGoldRatePerGram()) : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Gold Value:</span>")
            .append("<span class=\"info-value\">₹").append(goldLoan.getGoldValue() != null ? String.format("%.2f", goldLoan.getGoldValue()) : "N/A").append("</span>")
            .append("</div>");
        
        if (goldLoan.getGoldPurity() != null) {
            html.append("<div class=\"info-row\">")
                .append("<span class=\"info-label\">Gold Purity:</span>")
                .append("<span class=\"info-value\">").append(goldLoan.getGoldPurity()).append("</span>")
                .append("</div>");
        }
        
        html.append("</div>")
            .append("<div class=\"amount-section\">")
            .append("<h3 style=\"margin-top: 0; color: #856404;\">Loan Details</h3>")
            .append("<div class=\"amount-row\">")
            .append("<span>Loan Amount (75% of Gold Value):</span>")
            .append("<span>₹").append(goldLoan.getLoanAmount() != null ? String.format("%.2f", goldLoan.getLoanAmount()) : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"amount-row\">")
            .append("<span>Interest Rate:</span>")
            .append("<span>").append(goldLoan.getInterestRate() != null ? String.format("%.2f", goldLoan.getInterestRate()) : "12.00").append("% per annum</span>")
            .append("</div>")
            .append("<div class=\"amount-row\">")
            .append("<span>Tenure:</span>")
            .append("<span>").append(goldLoan.getTenure() != null ? goldLoan.getTenure() : "12").append(" months</span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"signature-section\">")
            .append("<div class=\"signature-box\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Customer Signature</div>")
            .append("</div>")
            .append("<div class=\"signature-box\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Authorized Signatory</div>")
            .append("<div class=\"signature-label\" style=\"margin-top: 5px;\">NeoBank Ltd.</div>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"footer\">")
            .append("<p><strong>This is a computer-generated receipt and does not require signature.</strong></p>")
            .append("<p>For any queries, contact us at: 1800 103 1906 | support@neobank.in</p>")
            .append("<p>© ").append(java.time.Year.now()).append(" NeoBank. All rights reserved.</p>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    public byte[] generatePersonalLoanReceipt(Loan loan) throws IOException {
        String html = generatePersonalLoanReceiptHtml(loan);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        
        return outputStream.toByteArray();
    }

    private String generatePersonalLoanReceiptHtml(Loan loan) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String currentDate = java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        String approvalDate = loan.getApprovalDate() != null ? 
            loan.getApprovalDate().format(formatter) : currentDate;
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>")
            .append("<html>")
            .append("<head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<title>Personal Loan Receipt - NeoBank</title>")
            .append("<style>")
            .append("body { font-family: Arial, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; position: relative; }")
            .append(".watermark { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) rotate(-45deg); font-size: 60px; color: rgba(30, 64, 175, 0.1); font-weight: bold; z-index: -1; pointer-events: none; white-space: nowrap; }")
            .append(".receipt-container { max-width: 800px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 15px rgba(0, 0, 0, 0.1); position: relative; z-index: 1; }")
            .append(".header { text-align: center; border-bottom: 3px solid #667eea; padding-bottom: 20px; margin-bottom: 30px; }")
            .append(".bank-logo { font-size: 28px; font-weight: bold; color: #667eea; margin-bottom: 10px; }")
            .append(".bank-name { font-size: 22px; color: #333; margin-bottom: 5px; }")
            .append(".bank-seal { position: absolute; top: 20px; right: 20px; width: 100px; height: 100px; border: 3px solid #667eea; border-radius: 50%; background: white; display: flex; flex-direction: column; align-items: center; justify-content: center; font-size: 10px; font-weight: bold; color: #667eea; text-align: center; padding: 10px; }")
            .append(".receipt-title { text-align: center; font-size: 20px; font-weight: bold; color: #333; margin-bottom: 30px; padding: 15px; background-color: #f8f9fa; border-radius: 5px; }")
            .append(".info-section { margin-bottom: 25px; padding: 20px; background-color: #f8f9fa; border-radius: 8px; }")
            .append(".info-row { display: flex; justify-content: space-between; margin-bottom: 12px; padding: 8px 0; border-bottom: 1px solid #e0e0e0; }")
            .append(".info-row:last-child { border-bottom: none; }")
            .append(".info-label { font-weight: bold; color: #555; font-size: 14px; }")
            .append(".info-value { color: #333; font-size: 14px; text-align: right; }")
            .append(".amount-section { background-color: #e3f2fd; padding: 20px; border-radius: 8px; margin: 20px 0; border: 2px solid #2196f3; }")
            .append(".amount-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 16px; }")
            .append(".amount-row.total { font-size: 20px; font-weight: bold; color: #1565c0; border-top: 2px solid #2196f3; padding-top: 10px; margin-top: 10px; }")
            .append(".footer { text-align: center; margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; color: #666; font-size: 12px; }")
            .append(".signature-section { display: flex; justify-content: space-between; margin-top: 30px; padding-top: 20px; border-top: 2px solid #e0e0e0; }")
            .append(".signature-box { text-align: center; flex: 1; }")
            .append(".signature-line { width: 200px; border-top: 2px solid #333; margin: 0 auto 5px; }")
            .append(".signature-label { font-size: 11px; color: #666; }")
            .append("</style>")
            .append("</head>")
            .append("<body>")
            .append("<div class=\"watermark\">NeoBank</div>")
            .append("<div class=\"receipt-container\">")
            .append("<div class=\"bank-seal\">")
            .append("<div>NEOBANK</div>")
            .append("<div style=\"font-size: 8px; margin-top: 3px;\">SEAL</div>")
            .append("<div style=\"font-size: 7px; margin-top: 2px;\">").append(currentDate).append("</div>")
            .append("</div>")
            .append("<div class=\"header\">")
            .append("<div class=\"bank-logo\">🏦 NeoBank</div>")
            .append("<div class=\"bank-name\">Personal Loan Receipt</div>")
            .append("</div>")
            .append("<div class=\"receipt-title\">Loan Approval Confirmation</div>")
            .append("<div class=\"info-section\">")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Receipt Number:</span>")
            .append("<span class=\"info-value\">PL-").append(loan.getId()).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Loan Account Number:</span>")
            .append("<span class=\"info-value\">").append(loan.getLoanAccountNumber() != null ? loan.getLoanAccountNumber() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Loan Type:</span>")
            .append("<span class=\"info-value\">").append(loan.getType() != null ? loan.getType() : "Personal Loan").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Approval Date:</span>")
            .append("<span class=\"info-value\">").append(approvalDate).append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Status:</span>")
            .append("<span class=\"info-value\"><strong style=\"color: #4caf50;\">APPROVED</strong></span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Customer Name:</span>")
            .append("<span class=\"info-value\">").append(loan.getUserName() != null ? loan.getUserName() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Account Number:</span>")
            .append("<span class=\"info-value\">").append(loan.getAccountNumber() != null ? loan.getAccountNumber() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Email:</span>")
            .append("<span class=\"info-value\">").append(loan.getUserEmail() != null ? loan.getUserEmail() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">PAN Number:</span>")
            .append("<span class=\"info-value\">").append(loan.getPan() != null ? loan.getPan() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">CIBIL Score:</span>")
            .append("<span class=\"info-value\">").append(loan.getCibilScore() != null ? loan.getCibilScore() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"info-row\">")
            .append("<span class=\"info-label\">Approved By:</span>")
            .append("<span class=\"info-value\">").append(loan.getApprovedBy() != null ? loan.getApprovedBy() : "Admin").append("</span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"amount-section\">")
            .append("<h3 style=\"margin-top: 0; color: #1565c0;\">Loan Details</h3>")
            .append("<div class=\"amount-row\">")
            .append("<span>Loan Amount:</span>")
            .append("<span>₹").append(loan.getAmount() != null ? String.format("%.2f", loan.getAmount()) : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"amount-row\">")
            .append("<span>Interest Rate:</span>")
            .append("<span>").append(loan.getInterestRate() != null ? String.format("%.2f", loan.getInterestRate()) : "N/A").append("% per annum</span>")
            .append("</div>")
            .append("<div class=\"amount-row\">")
            .append("<span>Tenure:</span>")
            .append("<span>").append(loan.getTenure() != null ? loan.getTenure() : "N/A").append(" months</span>")
            .append("</div>")
            .append("<div class=\"amount-row\">")
            .append("<span>Purpose:</span>")
            .append("<span>").append(loan.getPurpose() != null ? loan.getPurpose() : "N/A").append("</span>")
            .append("</div>")
            .append("<div class=\"amount-row total\">")
            .append("<span>Total Loan Amount Disbursed:</span>")
            .append("<span>₹").append(loan.getAmount() != null ? String.format("%.2f", loan.getAmount()) : "N/A").append("</span>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"signature-section\">")
            .append("<div class=\"signature-box\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Customer Signature</div>")
            .append("</div>")
            .append("<div class=\"signature-box\">")
            .append("<div class=\"signature-line\"></div>")
            .append("<div class=\"signature-label\">Authorized Signatory</div>")
            .append("<div class=\"signature-label\" style=\"margin-top: 5px;\">NeoBank Ltd.</div>")
            .append("</div>")
            .append("</div>")
            .append("<div class=\"footer\">")
            .append("<p><strong>This is a computer-generated receipt and does not require signature.</strong></p>")
            .append("<p>For any queries, contact us at: 1800 103 1906 | support@neobank.in</p>")
            .append("<p>© ").append(java.time.Year.now()).append(" NeoBank. All rights reserved.</p>")
            .append("</div>")
            .append("</div>")
            .append("</body>")
            .append("</html>");
        
        return html.toString();
    }

    // ======================== PASSBOOK GENERATION (SBI-Style) ========================

    public byte[] generatePassbook(Map<String, Object> accountInfo, List<Transaction> transactions) throws IOException {
        String html = generatePassbookHtml(accountInfo, transactions);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        HtmlConverter.convertToPdf(html, outputStream);
        return outputStream.toByteArray();
    }

    private String safeStr(Object val) {
        if (val == null) return "N/A";
        String s = String.valueOf(val);
        return s.isEmpty() ? "N/A" : s;
    }

    private String generatePassbookHtml(Map<String, Object> accountInfo, List<Transaction> transactions) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String issueDate = java.time.LocalDate.now().format(formatter);
        String accountNumber = safeStr(accountInfo.get("accountNumber"));
        String customerId = safeStr(accountInfo.get("customerId"));
        String name = safeStr(accountInfo.get("name"));
        String address = safeStr(accountInfo.get("address"));
        String phone = safeStr(accountInfo.get("phone"));
        String accountType = safeStr(accountInfo.getOrDefault("accountType", "Savings"));
        String branchName = safeStr(accountInfo.getOrDefault("branchName", "NeoBank Main Branch"));
        String branchCode = safeStr(accountInfo.getOrDefault("branchCode", "0001"));
        String ifscCode = safeStr(accountInfo.getOrDefault("ifscCode", "NEOB0001234"));
        String pan = safeStr(accountInfo.get("pan"));
        String aadharNumber = safeStr(accountInfo.get("aadharNumber"));
        String modeOfOperation = safeStr(accountInfo.getOrDefault("modeOfOperation", "SINGLE"));
        String occupation = safeStr(accountInfo.getOrDefault("occupation", ""));
        Double balance = accountInfo.get("balance") != null ? Double.parseDouble(String.valueOf(accountInfo.get("balance"))) : 0.0;
        String passbookNumber = "PB" + accountNumber;
        String ledgerNo = "LN" + (accountNumber.length() > 4 ? accountNumber.substring(accountNumber.length()-4) : accountNumber);

        // Mask Aadhar: show only last 4
        String maskedAadhar = aadharNumber.length() > 4
            ? "XXXX XXXX " + aadharNumber.substring(aadharNumber.length() - 4)
            : aadharNumber;

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>NeoBank Passbook</title>")
            .append("<style>")
            .append("@page { size: A4; margin: 0; }")
            .append("* { box-sizing: border-box; }")
            .append("body { font-family: Arial, 'Segoe UI', sans-serif; margin: 0; padding: 0; background: #fff; color: #000; font-size: 12px; }")

            // ======= COVER PAGE (SBI-style blue border, loan products) =======
            .append(".cover-page { width: 100%; height: 100vh; padding: 30px; page-break-after: always; }")
            .append(".cover-outer { border: 4px solid #1a4b8c; height: 95vh; }")
            // Top banner area with bank name
            .append(".cover-banner { background: linear-gradient(135deg, #1a4b8c 0%, #2563eb 100%); padding: 22px 30px; text-align: center; }")
            .append(".cover-banner-title { color: #fff; font-size: 20px; font-weight: 800; letter-spacing: 3px; text-transform: uppercase; }")
            .append(".cover-banner-sub { color: #93c5fd; font-size: 11px; letter-spacing: 2px; margin-top: 4px; }")
            // Loan products showcase
            .append(".cover-products { background: #eef4fb; padding: 20px 25px 15px; text-align: center; }")
            .append(".cover-products-heading { font-size: 14px; font-weight: 700; color: #1a4b8c; margin-bottom: 15px; letter-spacing: 1px; }")
            .append(".products-grid { width: 100%; }")
            .append(".product-cell { width: 25%; text-align: center; padding: 8px; }")
            .append(".product-icon { width: 100px; height: 70px; margin: 0 auto 6px; border: 2px solid #1a4b8c; background: #fff; text-align: center; line-height: 70px; font-size: 36px; color: #1a4b8c; }")
            .append(".product-label { font-size: 12px; font-weight: 700; color: #1a4b8c; }")
            // Bottom section: passbook title area
            .append(".cover-bottom { text-align: center; padding: 50px 30px 30px; }")
            .append(".cover-passbook-badge { border: 3px solid #1a4b8c; padding: 14px 55px; font-size: 24px; font-weight: 800; color: #1a4b8c; letter-spacing: 6px; text-transform: uppercase; display: inline-block; margin-bottom: 20px; }")
            .append(".cover-acc-type { font-size: 16px; color: #1a4b8c; font-weight: 700; text-transform: uppercase; letter-spacing: 3px; margin-bottom: 25px; }")
            .append(".cover-holder-name { font-size: 22px; font-weight: 700; color: #1a4b8c; text-align: center; }")
            .append(".cover-holder-acc { font-size: 14px; color: #555; margin-top: 6px; letter-spacing: 1px; text-align: center; }")
            .append(".cover-bank-footer { text-align: center; padding: 12px; font-size: 10px; color: #1a4b8c; border-top: 2px solid #1a4b8c; letter-spacing: 1px; }")

            // ======= PROFILE / DETAILS PAGE (SBI passbook inner page style) =======
            .append(".profile-page { width: 100%; min-height: 100vh; padding: 25px 30px; page-break-after: always; }")
            // Top header line with date, branch code
            .append(".pb-top-row { width: 100%; border-bottom: 2px solid #1a4b8c; padding-bottom: 8px; margin-bottom: 0; }")
            .append(".pb-top-left { font-size: 11px; line-height: 1.7; color: #333; }")
            .append(".pb-top-right { text-align: right; font-size: 11px; color: #1a4b8c; font-weight: 700; }")
            // Bank header with name + logo
            .append(".pb-bank-header { width: 100%; padding: 15px 0; border-bottom: 2px solid #1a4b8c; }")
            .append(".pb-bank-name-en { font-size: 22px; font-weight: 800; color: #1a4b8c; letter-spacing: 2px; }")
            .append(".pb-bank-tagline { font-size: 10px; color: #666; letter-spacing: 1px; margin-top: 2px; }")
            .append(".pb-bank-logo { width: 75px; height: 75px; border: 3px solid #1a4b8c; border-radius: 50%; text-align: center; line-height: 75px; }")
            .append(".pb-bank-logo-inner { font-size: 14px; font-weight: 900; color: #1a4b8c; }")
            // Passbook type title
            .append(".pb-type-title { text-align: center; padding: 12px 0; border-bottom: 1px solid #ccc; }")
            .append(".pb-type-title-text { font-size: 16px; font-weight: 800; color: #1a4b8c; letter-spacing: 3px; text-transform: uppercase; }")
            // Branch line
            .append(".pb-branch-line { text-align: center; padding: 8px 0; font-size: 14px; font-weight: 700; color: #333; border-bottom: 1px solid #ddd; }")
            // Details form (SBI-style with dotted underlines)
            .append(".pb-details { padding: 15px 0; }")
            .append(".pb-label { font-size: 11px; color: #555; font-weight: 600; width: 140px; vertical-align: bottom; padding-bottom: 4px; }")
            .append(".pb-label-hi { font-size: 9px; color: #888; }")
            .append(".pb-value { border-bottom: 1px dotted #999; padding-bottom: 2px; font-size: 12px; color: #000; font-weight: 600; padding-left: 8px; vertical-align: bottom; }")
            // Separator line
            .append(".pb-separator { border: none; border-top: 2px solid #1a4b8c; margin: 15px 0; }")
            // Branch Manager signature
            .append(".pb-sig-line { border-top: 1px solid #333; padding-top: 5px; font-size: 11px; color: #333; font-weight: 600; }")

            // ======= TRANSACTION PAGES =======
            .append(".txn-page { width: 100%; min-height: 100vh; padding: 20px 25px; }")
            .append(".txn-page-bank-name { font-size: 18px; font-weight: 800; color: #1a4b8c; letter-spacing: 2px; }")
            .append(".txn-acc-info { font-size: 11px; color: #555; text-align: right; line-height: 1.6; }")
            .append(".txn-balance-strip { background: #eef4fb; border: 1px solid #bdd4f0; padding: 10px 20px; }")
            .append(".txn-bal-lbl { font-size: 11px; color: #666; text-transform: uppercase; letter-spacing: 1px; }")
            .append(".txn-bal-val { font-size: 20px; font-weight: 800; color: #1a4b8c; }")
            // Transaction table (classic passbook style)
            .append(".txn-tbl { width: 100%; border-collapse: collapse; }")
            .append(".txn-tbl th { background: #1a4b8c; color: #fff; padding: 8px 10px; font-size: 10px; text-transform: uppercase; letter-spacing: 0.8px; text-align: left; font-weight: 700; border: 1px solid #15406e; }")
            .append(".txn-tbl td { padding: 7px 10px; border: 1px solid #ddd; font-size: 11px; color: #222; }")
            .append(".txn-tbl tr.even-row { background: #f5f8fc; }")
            .append(".cr { color: #0f7b3f; font-weight: 700; }")
            .append(".dr { color: #c0392b; font-weight: 700; }")
            .append(".bal { font-weight: 700; color: #1a4b8c; }")
            // Summary box
            .append(".txn-summary-box { margin-top: 18px; border: 2px solid #1a4b8c; padding: 14px 18px; }")
            .append(".summary-title { font-size: 13px; font-weight: 800; color: #1a4b8c; margin-bottom: 8px; text-transform: uppercase; letter-spacing: 1px; }")
            .append(".sum-row { padding: 4px 0; font-size: 12px; border-bottom: 1px dotted #ccc; }")
            .append(".sum-row-total { font-weight: 800; font-size: 13px; border-top: 2px solid #1a4b8c; margin-top: 6px; padding-top: 8px; }")
            .append(".txn-page-footer { margin-top: 25px; text-align: center; font-size: 10px; color: #999; border-top: 1px solid #ddd; padding-top: 10px; }")
            .append(".no-txn-msg { text-align: center; padding: 50px 20px; color: #aaa; font-size: 13px; }")
            .append("</style></head><body>");

        // ======================== PAGE 1: COVER (SBI-Style) ========================
        html.append("<div class=\"cover-page\">")
            .append("<div class=\"cover-outer\">")
            // Banner
            .append("<div class=\"cover-banner\">")
            .append("<div class=\"cover-banner-title\">NeoBank</div>")
            .append("<div class=\"cover-banner-sub\">Relationship Beyond Banking</div>")
            .append("</div>")
            // Loan Products using table for iText compatibility
            .append("<div class=\"cover-products\">")
            .append("<div class=\"cover-products-heading\">NeoBank - Attractive Loan Schemes With Lowest Interest Rates</div>")
            .append("<table class=\"products-grid\" cellspacing=\"0\" cellpadding=\"0\"><tr>")
            .append("<td class=\"product-cell\"><div class=\"product-icon\">&#127968;</div><div class=\"product-label\">Home Loan</div></td>")
            .append("<td class=\"product-cell\"><div class=\"product-icon\">&#128663;</div><div class=\"product-label\">Car Loan</div></td>")
            .append("<td class=\"product-cell\"><div class=\"product-icon\">&#127891;</div><div class=\"product-label\">Education Loan</div></td>")
            .append("<td class=\"product-cell\"><div class=\"product-icon\">&#128176;</div><div class=\"product-label\">Personal Loan</div></td>")
            .append("</tr></table></div>")
            // Bottom
            .append("<div class=\"cover-bottom\">")
            .append("<div class=\"cover-passbook-badge\">PASS BOOK</div><br/><br/>")
            .append("<div class=\"cover-acc-type\">").append(escapeHtml(accountType)).append(" Account</div><br/>")
            .append("<div class=\"cover-holder-name\">").append(escapeHtml(name)).append("</div>")
            .append("<div class=\"cover-holder-acc\">A/C No: ").append(escapeHtml(accountNumber)).append("</div>")
            .append("</div>")
            .append("<div class=\"cover-bank-footer\">NeoBank &bull; CIN: U65110MH2026PLC000001 &bull; TOLL FREE: 1800 103 1906 &bull; www.neobank.in</div>")
            .append("</div></div>");

        // ======================== PAGE 2: PROFILE (Exact SBI Passbook Inner Page) ========================
        html.append("<div class=\"profile-page\">")
            // Top row: date, code, branch using table for iText
            .append("<table class=\"pb-top-row\" cellspacing=\"0\" cellpadding=\"0\"><tr>")
            .append("<td class=\"pb-top-left\" style=\"width:70%;\">")
            .append(escapeHtml(issueDate)).append("&nbsp;&nbsp;&nbsp;")
            .append(escapeHtml(accountNumber)).append("&nbsp;&nbsp;&nbsp;")
            .append(escapeHtml(branchCode)).append("<br/>")
            .append(escapeHtml(branchName)).append(" ( ").append(escapeHtml(branchCode)).append(" )<br/>")
            .append("Mode of Operation : ").append(escapeHtml(modeOfOperation)).append("<br/>")
            .append("Nom.Reg No : ___________<br/>")
            .append("Date of Issue : ").append(escapeHtml(issueDate))
            .append("</td>")
            .append("<td class=\"pb-top-right\" style=\"width:30%;\">CONTINUATION</td>")
            .append("</tr></table>")

            // Bank header row with logo using table
            .append("<table class=\"pb-bank-header\" cellspacing=\"0\" cellpadding=\"0\"><tr>")
            .append("<td style=\"width:75%;\">")
            .append("<div class=\"pb-bank-name-en\">NeoBank</div>")
            .append("<div class=\"pb-bank-tagline\">Relationship Beyond Banking</div>")
            .append("</td>")
            .append("<td style=\"width:25%; text-align:right;\">")
            .append("<div class=\"pb-bank-logo\"><span class=\"pb-bank-logo-inner\">NEO BANK</span></div>")
            .append("</td></tr></table>")

            // Passbook type
            .append("<div class=\"pb-type-title\"><span class=\"pb-type-title-text\">").append(escapeHtml(accountType)).append(" BANK PASS BOOK</span></div>")

            // Branch
            .append("<div class=\"pb-branch-line\">BRANCH: ").append(escapeHtml(branchName)).append("</div>")

            // Details form (SBI two-column style) using tables for iText
            .append("<div class=\"pb-details\">")

            // Account holder name row
            .append("<table cellspacing=\"0\" cellpadding=\"4\" style=\"width:100%;margin-bottom:8px;\"><tr>")
            .append("<td class=\"pb-label\">Name(s)<br/><span class=\"pb-label-hi\">&#2344;&#2366;&#2350;</span></td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(name)).append("</td>")
            .append("</tr></table>")

            // Two column section using a main table
            .append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;\"><tr>")
            .append("<td style=\"width:50%;vertical-align:top;padding-right:10px;\">")

            // LEFT COLUMN
            .append("<table cellspacing=\"0\" cellpadding=\"4\" style=\"width:100%;\">")
            // Address
            .append("<tr><td class=\"pb-label\">Address<br/><span class=\"pb-label-hi\">&#2346;&#2340;&#2366;</span></td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(address)).append("</td></tr>")
            // Phone
            .append("<tr><td class=\"pb-label\">Phone</td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(phone)).append("</td></tr>")
            // Occupation
            .append("<tr><td class=\"pb-label\">Occupation<br/><span class=\"pb-label-hi\">&#2357;&#2381;&#2351;&#2357;&#2360;&#2366;&#2351;</span></td>")
            .append("<td class=\"pb-value\">").append(occupation.isEmpty() ? "&nbsp;" : escapeHtml(occupation)).append("</td></tr>")
            // PAN
            .append("<tr><td class=\"pb-label\">PAN</td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(pan)).append("</td></tr>")
            // Aadhaar
            .append("<tr><td class=\"pb-label\">Aadhaar No.<br/><span class=\"pb-label-hi\">&#2310;&#2343;&#2366;&#2352;</span></td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(maskedAadhar)).append("</td></tr>")
            .append("</table>")

            .append("</td>") // end left column
            .append("<td style=\"width:50%;vertical-align:top;padding-left:10px;border-left:1px solid #ccc;\">")

            // RIGHT COLUMN
            .append("<table cellspacing=\"0\" cellpadding=\"4\" style=\"width:100%;\">")
            // Pass Book No.
            .append("<tr><td class=\"pb-label\">Pass Book No.<br/><span class=\"pb-label-hi\">&#2346;&#2366;&#2360; &#2348;&#2369;&#2325; &#2325;&#2381;&#2352;.</span></td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(passbookNumber)).append("</td></tr>")
            // Ledger No.
            .append("<tr><td class=\"pb-label\">Ledger No.<br/><span class=\"pb-label-hi\">&#2326;&#2366;&#2340;&#2366;-&#2357;&#2361;&#2368; &#2325;&#2381;&#2352;.</span></td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(ledgerNo)).append("</td></tr>")
            // Account No.
            .append("<tr><td class=\"pb-label\">Account No.<br/><span class=\"pb-label-hi\">&#2326;&#2366;&#2340;&#2366; &#2325;&#2381;&#2352;.</span></td>")
            .append("<td class=\"pb-value\" style=\"font-size:14px;font-weight:800;color:#1a4b8c;\">").append(escapeHtml(accountNumber)).append("</td></tr>")
            // Customer ID
            .append("<tr><td class=\"pb-label\">Customer ID</td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(customerId)).append("</td></tr>")
            // IFSC Code
            .append("<tr><td class=\"pb-label\">IFSC Code</td>")
            .append("<td class=\"pb-value\">").append(escapeHtml(ifscCode)).append("</td></tr>")
            .append("</table>")

            .append("</td></tr></table>") // end two column table
            .append("</div>") // pb-details

            .append("<hr class=\"pb-separator\"/>")

            // Branch Manager signature (right aligned using table)
            .append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;margin-top:25px;\"><tr>")
            .append("<td style=\"width:60%;\">&nbsp;</td>")
            .append("<td style=\"width:40%;text-align:center;\">")
            .append("<br/><br/>")
            .append("<div class=\"pb-sig-line\">&#2358;&#2366;&#2326;&#2366; &#2346;&#2381;&#2352;&#2348;&#2306;&#2343;&#2325; / Branch Manager</div>")
            .append("</td></tr></table>")
            .append("</div>"); // profile-page

        // ======================== PAGE 3+: TRANSACTIONS ========================
        html.append("<div class=\"txn-page\">")
            .append("<table cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;border-bottom:2px solid #1a4b8c;padding-bottom:10px;margin-bottom:0;\"><tr>")
            .append("<td class=\"txn-page-bank-name\">NeoBank</td>")
            .append("<td class=\"txn-acc-info\">")
            .append("A/C No: ").append(escapeHtml(accountNumber)).append("<br/>")
            .append("Name: ").append(escapeHtml(name)).append("<br/>")
            .append("Branch: ").append(escapeHtml(branchName))
            .append("</td></tr></table>")
            .append("<table class=\"txn-balance-strip\" cellspacing=\"0\" cellpadding=\"0\" style=\"width:100%;\"><tr>")
            .append("<td class=\"txn-bal-lbl\">Current Balance</td>")
            .append("<td class=\"txn-bal-val\" style=\"text-align:right;\">&#8377; ").append(String.format("%,.2f", balance)).append("</td>")
            .append("</tr></table>");

        if (transactions != null && !transactions.isEmpty()) {
            html.append("<table class=\"txn-tbl\"><thead><tr>")
                .append("<th style=\"width:14%\">Date</th>")
                .append("<th style=\"width:28%\">Particulars</th>")
                .append("<th style=\"width:15%\">Chq. No / Ref.</th>")
                .append("<th style=\"width:14%\">Debit (&#8377;)</th>")
                .append("<th style=\"width:14%\">Credit (&#8377;)</th>")
                .append("<th style=\"width:15%\">Balance (&#8377;)</th>")
                .append("</tr></thead><tbody>");

            int rowIdx = 0;
            for (Transaction txn : transactions) {
                String txnDate = txn.getDate() != null ? txn.getDate().format(formatter) : "N/A";
                String txnRef = txn.getTransactionId() != null ? txn.getTransactionId() : "";
                String desc = txn.getDescription() != null ? txn.getDescription()
                    : (txn.getMerchant() != null ? txn.getMerchant() : "");
                boolean isCredit = "Credit".equalsIgnoreCase(txn.getType())
                    || "Deposit".equalsIgnoreCase(txn.getType())
                    || "Loan Credit".equalsIgnoreCase(txn.getType());
                String debitVal = isCredit ? "" : String.format("%,.2f", txn.getAmount() != null ? txn.getAmount() : 0.0);
                String creditVal = isCredit ? String.format("%,.2f", txn.getAmount() != null ? txn.getAmount() : 0.0) : "";
                String balVal = String.format("%,.2f", txn.getBalance() != null ? txn.getBalance() : 0.0);
                String rowClass = (rowIdx % 2 == 1) ? " class=\"even-row\"" : "";

                html.append("<tr").append(rowClass).append(">")
                    .append("<td>").append(txnDate).append("</td>")
                    .append("<td>").append(escapeHtml(desc)).append("</td>")
                    .append("<td>").append(escapeHtml(txnRef)).append("</td>")
                    .append("<td class=\"dr\">").append(debitVal).append("</td>")
                    .append("<td class=\"cr\">").append(creditVal).append("</td>")
                    .append("<td class=\"bal\">").append(balVal).append("</td>")
                    .append("</tr>");
                rowIdx++;
            }
            html.append("</tbody></table>");

            // Summary
            double totalCredit = transactions.stream()
                .filter(t -> "Credit".equalsIgnoreCase(t.getType()) || "Deposit".equalsIgnoreCase(t.getType()) || "Loan Credit".equalsIgnoreCase(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0).sum();
            double totalDebit = transactions.stream()
                .filter(t -> "Debit".equalsIgnoreCase(t.getType()) || "Withdraw".equalsIgnoreCase(t.getType()) || "Transfer".equalsIgnoreCase(t.getType()))
                .mapToDouble(t -> t.getAmount() != null ? t.getAmount() : 0.0).sum();

            html.append("<div class=\"txn-summary-box\">")
                .append("<div class=\"summary-title\">Transaction Summary</div>")
                .append("<table cellspacing=\"0\" cellpadding=\"4\" style=\"width:100%;\">")
                .append("<tr class=\"sum-row\"><td>Total Credits:</td><td class=\"cr\" style=\"text-align:right;\">+ &#8377; ").append(String.format("%,.2f", totalCredit)).append("</td></tr>")
                .append("<tr class=\"sum-row\"><td>Total Debits:</td><td class=\"dr\" style=\"text-align:right;\">- &#8377; ").append(String.format("%,.2f", totalDebit)).append("</td></tr>")
                .append("<tr class=\"sum-row\"><td>Total Transactions:</td><td style=\"text-align:right;\">").append(transactions.size()).append("</td></tr>")
                .append("<tr class=\"sum-row-total\"><td><b>Net Amount:</b></td><td style=\"text-align:right;\"><b>&#8377; ").append(String.format("%,.2f", totalCredit - totalDebit)).append("</b></td></tr>")
                .append("</table></div>");
        } else {
            html.append("<div class=\"no-txn-msg\">No transactions found for this account.</div>");
        }

        html.append("<div class=\"txn-page-footer\">")
            .append("<p>This is a computer-generated passbook and does not require a physical signature.</p>")
            .append("<p>For queries: 1800 103 1906 | support@neobank.in | www.neobank.in</p>")
            .append("<p>&copy; ").append(java.time.Year.now()).append(" NeoBank India Ltd. All rights reserved.</p>")
            .append("</div></div></body></html>");

        return html.toString();
    }

    private String escapeHtml(String input) {
        if (input == null) return "N/A";
        return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
