package com.neo.springapp.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PositivePayUpdateRequest(
        @NotNull @PastOrPresent LocalDate chequeDate,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String payeeName,
        String payeeAccountNumber,
        String payeeBankName,
        String payeeIfsc,
        String remarks,
        @AssertTrue(message = "Confirmation is required") boolean confirmation
) {}
