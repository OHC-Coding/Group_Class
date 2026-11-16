package com.ohc.groupclass.enrollment.enums;

public enum EnrollmentStatus {

    PENDING_PAYMENT,
    CONFIRMED,
    CANCELLED,
    REFUNDED;

    public boolean isOccupying() {
        return this == PENDING_PAYMENT || this == CONFIRMED;
    }
}
