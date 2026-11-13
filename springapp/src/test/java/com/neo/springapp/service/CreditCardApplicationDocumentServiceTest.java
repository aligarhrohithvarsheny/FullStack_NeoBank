package com.neo.springapp.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CreditCardApplicationDocumentServiceTest {

    @TempDir
    Path temporaryDirectory;

    private CreditCardApplicationDocumentService service;

    @BeforeEach
    void setUp() {
        service = new CreditCardApplicationDocumentService();
        ReflectionTestUtils.setField(service, "uploadDirectory", temporaryDirectory.toString());
    }

    @Test
    void storesImagesForAdminRetrievalAndDeletesThemOnRequest() throws Exception {
        byte[] image = validPng();
        MockMultipartFile pan = image("pan.png", image);
        MockMultipartFile aadhaar = image("aadhaar.png", image);
        MockMultipartFile selfie = image("selfie.png", image);
        String applicationId = "CC2026ABCDEF1234";

        service.store(applicationId, pan, aadhaar, selfie);

        assertTrue(service.load(applicationId, "pan").exists());
        assertEquals("image/png", service.contentType(service.load(applicationId, "aadhaar")));
        assertThrows(IllegalArgumentException.class, () -> service.load(applicationId, "../pan"));
        service.delete(applicationId);
        assertFalse(Files.exists(temporaryDirectory.resolve(applicationId)));
    }

    @Test
    void rejectsNonImageFiles() {
        MockMultipartFile invalid = image("document.txt", new byte[]{1, 2, 3});

        assertThrows(IllegalArgumentException.class,
                () -> service.store("CC2026ABCDEF1234", invalid, invalid, invalid));
    }

    private MockMultipartFile image(String name, byte[] bytes) {
        return new MockMultipartFile(name, name, "image/png", bytes);
    }

    private byte[] validPng() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
