package com.neo.springapp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
        String fileName = String.format("fastag_%s_%s.png", fasttagId == null ? barcodeNumber : fasttagId, timestamp);
        File outFile = new File(dir, fileName);

        String safeBankName = (bankName == null || bankName.isBlank()) ? "NeoBank" : bankName;
        String safeCustomerName = (customerName == null || customerName.isBlank()) ? "FASTag User" : customerName;
        String safeVehicleNumber = (vehicleNumber == null || vehicleNumber.isBlank()) ? "NA" : vehicleNumber.toUpperCase();
        String safeFasttagId = (fasttagId == null || fasttagId.isBlank()) ? "NA" : fasttagId;
        String safeBarcode = (barcodeNumber == null || barcodeNumber.isBlank()) ? generateBarcodeDigits(12) : barcodeNumber;

        BufferedImage card = new BufferedImage(920, 540, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = card.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.setPaint(new GradientPaint(0, 0, new Color(245, 247, 255), 920, 540, new Color(228, 238, 255)));
        g.fillRoundRect(0, 0, 920, 540, 28, 28);
        g.setColor(new Color(52, 73, 170));
        g.setStroke(new BasicStroke(4f));
        g.drawRoundRect(2, 2, 916, 536, 26, 26);

        g.setPaint(new GradientPaint(0, 0, new Color(38, 64, 179), 920, 0, new Color(89, 43, 140)));
        g.fillRoundRect(26, 24, 868, 62, 18, 18);

        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 38));
        g.drawString("FASTag", 48, 66);
        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        g.drawString(safeBankName, 690, 66);

        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("SansSerif", Font.BOLD, 24));
        g.drawString("Customer: " + safeCustomerName, 48, 140);
        g.drawString("Vehicle: " + safeVehicleNumber, 48, 176);

        g.setColor(new Color(71, 85, 105));
        g.setFont(new Font("Monospaced", Font.BOLD, 23));
        g.drawString("FASTag ID: " + safeFasttagId, 48, 224);

        Code128Writer writer = new Code128Writer();
        BitMatrix bitMatrix = writer.encode(safeBarcode, BarcodeFormat.CODE_128, 780, 125);
        BufferedImage barcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
        g.drawImage(barcodeImage, 70, 260, null);

        g.setColor(new Color(15, 23, 42));
        g.setFont(new Font("Monospaced", Font.BOLD, 26));
        g.drawString(safeBarcode, 300, 420);

        g.setColor(new Color(51, 65, 85));
        g.setFont(new Font("SansSerif", Font.PLAIN, 19));
        g.drawString("Issue Date: " + issueDate.toLocalDate(), 48, 468);
        g.drawString("Approved FASTag - Download from NeoBank Dashboard", 48, 500);

        g.dispose();
        ImageIO.write(card, "png", outFile);

        // return relative path
        return outFile.getAbsolutePath();
    }

    private String generateBarcodeDigits(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }
}
