package com.neo.springapp.dto;
import jakarta.validation.constraints.NotBlank;
public record PositivePayRejectionRequest(@NotBlank String reason, String performedBy) {}
