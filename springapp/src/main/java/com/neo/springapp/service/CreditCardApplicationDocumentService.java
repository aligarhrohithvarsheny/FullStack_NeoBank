package com.neo.springapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class CreditCardApplicationDocumentService {

    private static final long MAX_DOCUMENT_SIZE = 5 * 1024 * 1024;
    private static final String[] DOCUMENT_TYPES = {"pan", "aadhaar", "selfie"};

    @Value("${neobank.credit-card-applications.upload-dir:uploads/credit-card-applications}")
    private String uploadDirectory = "uploads/credit-card-applications";

    public void store(String applicationId, MultipartFile pan, MultipartFile aadhaar, MultipartFile selfie)
            throws IOException {
        byte[] panBytes = validateImage(pan, "PAN document");
        byte[] aadhaarBytes = validateImage(aadhaar, "Aadhaar document");
        byte[] selfieBytes = validateImage(selfie, "Live selfie");
        Path directory = applicationDirectory(applicationId);
        Files.createDirectories(directory);
        try {
            Files.write(directory.resolve("pan" + extension(panBytes)), panBytes);
            Files.write(directory.resolve("aadhaar" + extension(aadhaarBytes)), aadhaarBytes);
            Files.write(directory.resolve("selfie" + extension(selfieBytes)), selfieBytes);
        } catch (IOException exception) {
            delete(applicationId);
            throw exception;
        }
    }

    public Resource load(String applicationId, String documentType) {
        if (!isValidApplicationId(applicationId) || !isValidDocumentType(documentType)) {
            throw new IllegalArgumentException("Invalid application document.");
        }
        Path directory = applicationDirectory(applicationId);
        for (String extension : new String[]{".jpg", ".png"}) {
            Path file = directory.resolve(documentType + extension).normalize();
            if (file.startsWith(directory) && Files.isRegularFile(file)) {
                return new FileSystemResource(file);
            }
        }
        throw new IllegalArgumentException("The requested application document is unavailable.");
    }

    public String contentType(Resource resource) {
        String filename = resource.getFilename();
        return filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".png")
                ? "image/png" : "image/jpeg";
    }

    public void delete(String applicationId) throws IOException {
        if (!isValidApplicationId(applicationId)) return;
        Path directory = applicationDirectory(applicationId);
        if (!Files.exists(directory)) return;
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private byte[] validateImage(MultipartFile file, String label) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException(label + " is required and must be 5 MB or smaller.");
        }
        byte[] bytes = file.getBytes();
        boolean png = bytes.length >= 8 && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
        boolean jpeg = bytes.length >= 3 && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        if ((!png && !jpeg) || ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
            throw new IllegalArgumentException(label + " must be a valid JPEG or PNG image.");
        }
        return bytes;
    }

    private String extension(byte[] bytes) {
        return (bytes[0] & 0xFF) == 0x89 ? ".png" : ".jpg";
    }

    private Path applicationDirectory(String applicationId) {
        if (!isValidApplicationId(applicationId)) throw new IllegalArgumentException("Invalid application ID.");
        Path root = Paths.get(uploadDirectory).toAbsolutePath().normalize();
        Path directory = root.resolve(applicationId).normalize();
        if (!directory.startsWith(root)) throw new IllegalArgumentException("Invalid application ID.");
        return directory;
    }

    private boolean isValidApplicationId(String applicationId) {
        return applicationId != null && applicationId.matches("CC[0-9]{4}[A-F0-9]{10}");
    }

    private boolean isValidDocumentType(String documentType) {
        if (documentType == null) return false;
        for (String type : DOCUMENT_TYPES) {
            if (type.equals(documentType)) return true;
        }
        return false;
    }
}
