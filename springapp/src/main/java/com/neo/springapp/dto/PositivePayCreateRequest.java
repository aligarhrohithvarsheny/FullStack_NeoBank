package com.neo.springapp.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PositivePayCreateRequest(
        @NotBlank String accountNumber,
        @NotBlank String chequeNumber,
        @NotNull @PastOrPresent LocalDate chequeDate,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank String payeeName,
        String payeeAccountNumber,
        String payeeBankName,
        String payeeIfsc,
        String remarks,
        @AssertTrue(message = "Confirmation is required") boolean confirmation
) {}
