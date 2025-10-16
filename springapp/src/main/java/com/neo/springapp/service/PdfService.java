package com.neo.springapp.service;

import com.itextpdf.html2pdf.HtmlConverter;
import com.neo.springapp.model.TransferRecord;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {

    public byte[] generateTransferReceipt(TransferRecord transfer) throws IOException {
        String html = generateTransferReceiptHtml(transfer);
        
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
}
