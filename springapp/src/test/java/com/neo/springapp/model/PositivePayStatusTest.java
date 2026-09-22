package com.neo.springapp.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositivePayStatusTest {

    @Test
    void registrationStatusesIncludeMatchedAndMismatch() {
        assertTrue(PositivePayStatus.isRegistrationStatus(PositivePayStatus.PENDING_ADMIN_APPROVAL));
        assertTrue(PositivePayStatus.isRegistrationStatus(PositivePayStatus.APPROVED));
        assertTrue(PositivePayStatus.isRegistrationStatus(PositivePayStatus.MATCHED));
        assertTrue(PositivePayStatus.isRegistrationStatus(PositivePayStatus.MISMATCH));

        assertFalse(PositivePayStatus.isRegistrationStatus(PositivePayStatus.REJECTED));
        assertFalse(PositivePayStatus.isRegistrationStatus(PositivePayStatus.CANCELLED));
    }
}
