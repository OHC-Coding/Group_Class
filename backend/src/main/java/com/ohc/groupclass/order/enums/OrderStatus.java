package com.ohc.groupclass.order.enums;

public enum OrderStatus {

    PAYING,
    PAID,
    CLOSED,
    REFUNDED;

    public static OrderStatus of(String value) {
        try {
            return OrderStatus.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException e) {
            return null;
        }
    }
}
