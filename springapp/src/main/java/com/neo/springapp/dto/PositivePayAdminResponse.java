package com.neo.springapp.dto;

import com.neo.springapp.model.PositivePayRequest;

public record PositivePayAdminResponse(PositivePayResponse request, String accountStatus, String fullAccountNumber, String customerEmail, String customerPhone) {
    public static PositivePayAdminResponse from(PositivePayRequest r, String accountStatus) {
        return new PositivePayAdminResponse(PositivePayResponse.from(r), accountStatus, r.getAccountNumber(), r.getCustomerEmail(), r.getCustomerPhone());
    }
}
