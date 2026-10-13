package com.app.models;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Order state machine.
 * Valid transitions:
 *   PLACED -> ACCEPTED
 *   PLACED -> CANCELLED (customer cancellation)
 *   PLACED -> REJECTED  (restaurant rejection)
 *   ACCEPTED -> PREPARING
 *   ACCEPTED -> CANCELLED
 *   PREPARING -> READY_FOR_PICKUP
 *   READY_FOR_PICKUP -> OUT_FOR_DELIVERY
 *   OUT_FOR_DELIVERY -> DELIVERED
 */
public enum OrderStatus {
    PLACED("placed"),
    ACCEPTED("accepted"),
    PREPARING("preparing"),
    READY_FOR_PICKUP("ready_for_pickup"),
    OUT_FOR_DELIVERY("out_for_delivery"),
    DELIVERED("delivered"),
    CANCELLED("cancelled"),
    REJECTED("rejected");

    private final String dbValue;

    OrderStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() { return dbValue; }

    public static OrderStatus fromDbValue(String value) {
        for (OrderStatus s : values()) {
            if (s.dbValue.equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown order status: " + value);
    }

    public boolean canTransitionTo(OrderStatus next) {
        switch (this) {
            case PLACED: return next == ACCEPTED || next == CANCELLED || next == REJECTED;
            case ACCEPTED: return next == PREPARING || next == CANCELLED;
            case PREPARING: return next == READY_FOR_PICKUP;
            case READY_FOR_PICKUP: return next == OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY: return next == DELIVERED;
            default: return false;
        }
    }

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED || this == REJECTED;
    }
}
