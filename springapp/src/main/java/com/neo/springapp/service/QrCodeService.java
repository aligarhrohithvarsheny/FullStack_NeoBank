package com.neo.springapp.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QrCodeService {
    
    // Store QR sessions: token -> QrSessionData
    private final Map<String, QrSessionData> qrSessions = new ConcurrentHashMap<>();
    
    // QR code expiration time in milliseconds (5 minutes)
    private static final long QR_EXPIRATION_TIME = 5 * 60 * 1000;
    
    // QR code size
    private static final int QR_CODE_SIZE = 300;
    
    /**
     * Generate a new QR code session
     * @return QR session token
     */
    public String generateQrSession() {
        String token = UUID.randomUUID().toString();
        QrSessionData sessionData = new QrSessionData(token, System.currentTimeMillis());
        qrSessions.put(token, sessionData);
        
        // Clean up expired sessions periodically
        cleanupExpiredSessions();
        
        return token;
    }
    
    /**
     * Generate QR code image as Base64 string
     * @param token QR session token
     * @param loginUrl URL to open on mobile (with token parameter)
     * @return Base64 encoded PNG image
     */
    public String generateQrCodeImage(String token, String loginUrl) {
        try {
            String qrContent = loginUrl + "?qrToken=" + token;
            
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);
            
            BufferedImage image = new BufferedImage(QR_CODE_SIZE, QR_CODE_SIZE, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();
            
            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, QR_CODE_SIZE, QR_CODE_SIZE);
            graphics.setColor(Color.BLACK);
            
            for (int i = 0; i < QR_CODE_SIZE; i++) {
                for (int j = 0; j < QR_CODE_SIZE; j++) {
                    if (bitMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        } catch (WriterException | IOException e) {
            System.err.println("Error generating QR code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Get QR session data
     * @param token QR session token
     * @return QrSessionData or null if not found/expired
     */
    public QrSessionData getQrSession(String token) {
        QrSessionData session = qrSessions.get(token);
        if (session == null) {
            return null;
        }
        
        // Check if expired
        if (System.currentTimeMillis() - session.getCreatedAt() > QR_EXPIRATION_TIME) {
            qrSessions.remove(token);
            return null;
        }
        
        return session;
    }
    
    /**
     * Update QR session status
     * @param token QR session token
     * @param status New status (PENDING, SCANNED, LOGGED_IN, EXPIRED)
     * @param userData User data if logged in
     */
    public void updateQrSession(String token, String status, Object userData) {
        QrSessionData session = qrSessions.get(token);
        if (session != null) {
            session.setStatus(status);
            session.setUserData(userData);
            session.setUpdatedAt(System.currentTimeMillis());
        }
    }
    
    /**
     * Remove QR session
     * @param token QR session token
     */
    public void removeQrSession(String token) {
        qrSessions.remove(token);
    }
    
    /**
     * Generate UPI payment QR code
     * @param upiId UPI ID (e.g., phone@paytm, account@neobank)
     * @param name Payee name
     * @param amount Amount (optional, null for dynamic amount)
     * @param transactionNote Transaction note (optional)
     * @return Base64 encoded PNG image of QR code
     */
    public String generateUpiQrCode(String upiId, String name, Double amount, String transactionNote) {
        try {
            // Validate inputs
            if (upiId == null || upiId.isEmpty()) {
                System.err.println("Error: UPI ID is null or empty");
                return null;
            }
            
            // Build UPI payment URL
            // Format: upi://pay?pa=<UPI_ID>&pn=<NAME>&am=<AMOUNT>&cu=INR&tn=<NOTE>
            StringBuilder upiUrl = new StringBuilder("upi://pay?pa=");
            upiUrl.append(upiId);
            
            // URL encode name properly
            if (name != null && !name.isEmpty()) {
                try {
                    String encodedName = java.net.URLEncoder.encode(name, "UTF-8");
                    upiUrl.append("&pn=").append(encodedName);
                } catch (java.io.UnsupportedEncodingException e) {
                    // Fallback to simple replacement
                    upiUrl.append("&pn=").append(name.replace(" ", "%20"));
                }
            }
            
            if (amount != null && amount > 0) {
                upiUrl.append("&am=").append(String.format("%.2f", amount));
            }
            upiUrl.append("&cu=INR");
            
            // URL encode transaction note properly
            if (transactionNote != null && !transactionNote.isEmpty()) {
                try {
                    String encodedNote = java.net.URLEncoder.encode(transactionNote, "UTF-8");
                    upiUrl.append("&tn=").append(encodedNote);
                } catch (java.io.UnsupportedEncodingException e) {
                    // Fallback to simple replacement
                    upiUrl.append("&tn=").append(transactionNote.replace(" ", "%20"));
                }
            }
            
            String qrContent = upiUrl.toString();
            System.out.println("Generated UPI URL: " + qrContent);
            
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(qrContent, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE, hints);
            
            BufferedImage image = new BufferedImage(QR_CODE_SIZE, QR_CODE_SIZE, BufferedImage.TYPE_INT_RGB);
            image.createGraphics();
            
            Graphics2D graphics = (Graphics2D) image.getGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, QR_CODE_SIZE, QR_CODE_SIZE);
            graphics.setColor(Color.BLACK);
            
            for (int i = 0; i < QR_CODE_SIZE; i++) {
                for (int j = 0; j < QR_CODE_SIZE; j++) {
                    if (bitMatrix.get(i, j)) {
                        graphics.fillRect(i, j, 1, 1);
                    }
                }
            }
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            byte[] imageBytes = baos.toByteArray();
            
            if (imageBytes == null || imageBytes.length == 0) {
                System.err.println("Error: Generated image bytes are null or empty");
                return null;
            }
            
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String result = "data:image/png;base64," + base64Image;
            
            System.out.println("QR code image generated successfully. Size: " + imageBytes.length + " bytes");
            return result;
        } catch (WriterException e) {
            System.err.println("WriterException generating UPI QR code: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            System.err.println("IOException generating UPI QR code: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("Unexpected error generating UPI QR code: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Clean up expired sessions
     */
    private void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        qrSessions.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().getCreatedAt() > QR_EXPIRATION_TIME
        );
    }
    
    /**
     * Inner class to store QR session data
     */
    public static class QrSessionData {
        private String token;
        private String status; // PENDING, SCANNED, LOGGED_IN, EXPIRED
        private long createdAt;
        private long updatedAt;
        private Object userData;
        
        public QrSessionData(String token, long createdAt) {
            this.token = token;
            this.status = "PENDING";
            this.createdAt = createdAt;
            this.updatedAt = createdAt;
        }
        
        public String getToken() {
            return token;
        }
        
        public void setToken(String token) {
            this.token = token;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }
        
        public long getUpdatedAt() {
            return updatedAt;
        }
        
        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }
        
        public Object getUserData() {
            return userData;
        }
        
        public void setUserData(Object userData) {
            this.userData = userData;
        }
    }
}

