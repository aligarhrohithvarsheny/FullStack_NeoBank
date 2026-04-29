package com.neo.springapp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

import javax.imageio.ImageIO;

@Component
public class FasttagStickerGenerator {

    public String generateBarcodeImageBase64(String barcodeText, int width, int height) throws Exception {
        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(barcodeText, BarcodeFormat.CODE_128, width, height);
        BufferedImage img = MatrixToImageWriter.toBufferedImage(bitMatrix);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    public String generateStickerPdf(String fasttagId, String barcodeNumber, String customerName, String vehicleNumber, String bankName, LocalDateTime issueDate, String outputDir) throws Exception {
        // create output dir
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();

        String timestamp = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(issueDate);
        String fileName = String.format("fastag_%s_%s.pdf", fasttagId == null ? barcodeNumber : fasttagId, timestamp);
        File outFile = new File(dir, fileName);

        String barcodeBase64 = generateBarcodeImageBase64(barcodeNumber, 400, 80);

        // Simple HTML layout resembling FASTag sticker
        String html = "<html><head><style>body{font-family:Arial,sans-serif;} .sticker{width:400px;height:200px;border:2px solid #333;padding:8px;display:block;} .top{display:flex;justify-content:space-between;align-items:center;} .center{margin-top:6px;text-align:center;} .barcode{margin-top:6px;} .meta{font-size:12px;margin-top:6px}</style></head><body>"
                + "<div class='sticker'>"
                + "<div class='top'><div style='text-align:center;flex:1'><strong>FASTag</strong></div><div style='text-align:right;flex:1'><strong>" + bankName + "</strong></div></div>"
                + "<div class='center'><div style='font-weight:600;font-size:14px'>" + customerName + "</div><div style='font-size:12px'>" + vehicleNumber + "</div></div>"
                + "<div class='barcode' style='text-align:center'><img src='data:image/png;base64," + barcodeBase64 + "' alt='barcode' /></div>"
                + "<div class='meta'><div><strong>FASTag ID:</strong> " + fasttagId + "</div><div><strong>Barcode:</strong> " + barcodeNumber + "</div><div><strong>Issue Date:</strong> " + issueDate.toLocalDate().toString() + "</div></div>"
                + "</div></body></html>";

        // convert html to pdf
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            ConverterProperties props = new ConverterProperties();
            HtmlConverter.convertToPdf(html, fos, props);
        }

        // return relative path
        return outFile.getAbsolutePath();
    }
}
