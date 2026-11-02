package com.neo.springapp.controller;

import com.neo.springapp.dto.BankingProductConfigRequest;
import com.neo.springapp.model.BankingProductConfig;
import com.neo.springapp.model.SavingsFeature;
import com.neo.springapp.repository.BankingProductConfigRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class BankingProductConfigControllerTest {

    @Test
    void savingsProductCannotDisableFeaturesThroughProductConfiguration() {
        BankingProductConfigRepository repository = mock(BankingProductConfigRepository.class);
        BankingProductConfigController controller = new BankingProductConfigController(repository);
        BankingProductConfigRequest request = new BankingProductConfigRequest(
                "SAVINGS-PLUS", "Savings Plus", "SAVINGS", 3.5, 1000.0, 50000.0,
                200000.0, 0.0, "Eligible customers", List.of("accounts"),
                LocalDate.now(), "admin");
        when(repository.save(any(BankingProductConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        controller.propose(request);

        var product = org.mockito.ArgumentCaptor.forClass(BankingProductConfig.class);
        verify(repository).save(product.capture());
        assertThat(product.getValue().getEnabledFeatures()).containsExactlyElementsOf(SavingsFeature.IDS);
    }
}
