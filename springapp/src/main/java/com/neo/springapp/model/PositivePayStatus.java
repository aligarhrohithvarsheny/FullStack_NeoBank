package com.neo.springapp.model;

public enum PositivePayStatus {
    PENDING_ADMIN_APPROVAL,
    APPROVED,
    REJECTED,
    CANCELLED,
    MATCHED,
    MISMATCH;

    public boolean isRegistrationStatus() {
        return isRegistrationStatus(this);
    }

    public static boolean isRegistrationStatus(PositivePayStatus status) {
        return status == PENDING_ADMIN_APPROVAL
                || status == APPROVED
                || status == MATCHED
                || status == MISMATCH;
    }
}
