package com.neo.springapp.service;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BankFormServiceTest {

    @Test
    void resolveUploadDirectoryUsesConfiguredPath() {
        Path path = BankFormService.resolveUploadDirectory("uploads/bank-forms");

        assertThat(path).isAbsolute();
        assertThat(path.toString().replace('\\', '/')).endsWith("uploads/bank-forms");
    }

    @Test
    void resolveUploadDirectoryFallsBackToDefault() {
        Path path = BankFormService.resolveUploadDirectory(null);

        assertThat(path).isAbsolute();
        assertThat(path.toString().replace('\\', '/')).endsWith("uploads/bank-forms");
    }
}
