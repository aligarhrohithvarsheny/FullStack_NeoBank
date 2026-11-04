package com.neo.springapp.service;

import com.neo.springapp.model.HomeLoan;
import com.neo.springapp.repository.HomeLoanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HomeLoanDocumentAvailabilityTest {

    @TempDir
    Path temporaryDirectory;

    private HomeLoanService service;
    private HomeLoanRepository repository;

    @BeforeEach
    void setUp() {
        service = new HomeLoanService();
        repository = mock(HomeLoanRepository.class);
        ReflectionTestUtils.setField(service, "repo", repository);
        ReflectionTestUtils.setField(service, "uploadRoot", temporaryDirectory.resolve("uploads").toString());
    }

    @Test
    void accountListingExposesOnlyDocumentsThatCanBeDownloaded() throws Exception {
        Path documentDirectory = temporaryDirectory.resolve("uploads").resolve("home-loan-documents");
        Files.createDirectories(documentDirectory);
        Files.writeString(documentDirectory.resolve("available-signature.pdf"), "signature");

        HomeLoan loan = new HomeLoan();
        loan.setSignaturePath("available-signature.pdf");
        loan.setFdReceiptPath("missing-receipt.pdf");
        when(repository.findByAccountNumberOrderByApplicationDateDesc("ACC-1")).thenReturn(List.of(loan));

        List<HomeLoan> loans = service.byAccount("ACC-1");

        assertThat(loans).hasSize(1);
        assertThat(loans.get(0).getAvailableDocuments())
                .singleElement()
                .satisfies(document -> {
                    assertThat(document.get("type")).isEqualTo("signature");
                    assertThat(document.get("name")).isEqualTo("available-signature.pdf");
                });
    }

    @Test
    void documentDownloadUsesLightweightPathProjectionAndReturnsTheFile() throws Exception {
        Path documentDirectory = temporaryDirectory.resolve("uploads").resolve("home-loan-documents");
        Files.createDirectories(documentDirectory);
        Path receipt = Files.writeString(documentDirectory.resolve("receipt.pdf"), "receipt");
        HomeLoanRepository.HomeLoanDocumentPaths paths = mock(HomeLoanRepository.HomeLoanDocumentPaths.class);
        when(paths.getFdReceiptPath()).thenReturn("receipt.pdf");
        when(repository.findDocumentPathsById(12L)).thenReturn(Optional.of(paths));

        assertThat(service.documentFile(12L, "fdReceipt", null).get("path")).isEqualTo(receipt);

        verify(repository).findDocumentPathsById(12L);
        verify(repository, never()).findById(12L);
    }

    @Test
    void missingOrInvalidDocumentsAreNotDownloadable() {
        HomeLoanRepository.HomeLoanDocumentPaths paths = mock(HomeLoanRepository.HomeLoanDocumentPaths.class);
        when(paths.getSignaturePath()).thenReturn("missing-signature.pdf");
        when(repository.findDocumentPathsById(12L)).thenReturn(Optional.of(paths));

        assertThatThrownBy(() -> service.documentFile(12L, "signature", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not available");
        assertThatThrownBy(() -> service.documentFile(12L, "unknown", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown document type");
    }
}
