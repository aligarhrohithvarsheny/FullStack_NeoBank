package com.neo.springapp.controller;

import com.neo.springapp.model.GoldLoan;
import com.neo.springapp.service.GoldLoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.core.io.Resource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoldLoanControllerJewelleryImageTest {

    @Mock
    private GoldLoanService goldLoanService;

    @TempDir
    private Path temporaryDirectory;

    private GoldLoanController controller;
    private GoldLoan loan;

    @BeforeEach
    void setUp() {
        controller = new GoldLoanController();
        ReflectionTestUtils.setField(controller, "goldLoanService", goldLoanService);
        ReflectionTestUtils.setField(controller, "uploadDirectory", temporaryDirectory.toString());
        loan = new GoldLoan();
        when(goldLoanService.getGoldLoanById(17L)).thenReturn(loan);
    }

    @Test
    void uploadedImageCanBeReadFromConfiguredUploadDirectory() throws Exception {
        byte[] imageBytes = new byte[] {1, 2, 3, 4};
        MockMultipartFile image = new MockMultipartFile("imageOne", "jewellery.png", "image/png", imageBytes);
        when(goldLoanService.save(loan)).thenReturn(loan);

        ResponseEntity<?> uploadResponse = controller.uploadJewelleryImages(17L, image, null);
        ResponseEntity<?> imageResponse = controller.viewJewelleryImage(17L, "one");

        assertThat(uploadResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loan.getGoldImageOnePath()).startsWith("gold-loans");
        assertThat(imageResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        try (var inputStream = ((Resource) imageResponse.getBody()).getInputStream()) {
            assertThat(inputStream.readAllBytes()).isEqualTo(imageBytes);
        }
    }

    @Test
    void unsupportedImageSlotReturnsBadRequest() {
        ResponseEntity<?> response = controller.viewJewelleryImage(17L, "three");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
