package com.neo.springapp.dto;

import com.neo.springapp.model.PositivePayRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PositivePayResponse(String referenceNumber, Long userId, String accountNumber, String accountType,
                                  String accountHolderName, String chequeNumber, String chequeStatus,
                                  String chequeBookNumber, LocalDate chequeDate, BigDecimal amount,
                                  String payeeName, String payeeAccountNumber, String payeeBankName,
                                  String payeeIfsc, String remarks, String status, LocalDateTime submittedAt,
                                  LocalDateTime approvedAt, String approvedBy, String rejectionReason,
                                  String customerEmail, String customerPhone) {
    public static PositivePayResponse from(PositivePayRequest r) {
        return new PositivePayResponse(r.getReferenceNumber(), r.getUserId(), mask(r.getAccountNumber()), r.getAccountType(), r.getAccountHolderName(), r.getChequeNumber(), r.getChequeStatus(), r.getChequeBookNumber(), r.getChequeDate(), r.getAmount(), r.getPayeeName(), r.getPayeeAccountNumber(), r.getPayeeBankName(), r.getPayeeIfsc(), r.getRemarks(), r.getStatus().name(), r.getSubmittedAt(), r.getApprovedAt(), r.getApprovedBy(), r.getRejectionReason(), r.getCustomerEmail(), r.getCustomerPhone());
    }
    private static String mask(String value) { if (value == null || value.length() < 4) return value; return "X".repeat(Math.max(0, value.length() - 4)) + value.substring(value.length() - 4); }
}
