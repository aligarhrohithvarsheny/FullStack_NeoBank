package com.neo.springapp.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccountConversionServiceTest {

    @Test
    void supportsSavingsToSalaryConversion() {
        assertThat(AccountConversionService.isSupportedConversion("Savings", "Salary")).isTrue();
    }

    @Test
    void rejectsUnsupportedAccountTypeMappings() {
        assertThat(AccountConversionService.isSupportedConversion("Savings", "Current")).isFalse();
    }

    @Test
    void normalizesAccountTypeNames() {
        assertThat(AccountConversionService.normalizeSourceType("salary")).isEqualTo("Salary");
        assertThat(AccountConversionService.normalizeTargetType("joint")).isEqualTo("Joint");
    }
}
