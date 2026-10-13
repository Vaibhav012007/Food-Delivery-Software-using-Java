package com.app.test;

import com.app.models.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Order Status State Machine Tests")
public class OrderStatusStateMachineTest {

    @Test
    @DisplayName("Valid transitions from PLACED")
    void testPlacedTransitions() {
        OrderStatus placed = OrderStatus.PLACED;
        assertTrue(placed.canTransitionTo(OrderStatus.ACCEPTED), "PLACED should be able to transition to ACCEPTED");
        assertTrue(placed.canTransitionTo(OrderStatus.CANCELLED), "PLACED should be able to transition to CANCELLED");
        assertTrue(placed.canTransitionTo(OrderStatus.REJECTED), "PLACED should be able to transition to REJECTED");
        assertFalse(placed.canTransitionTo(OrderStatus.PREPARING), "PLACED cannot jump directly to PREPARING");
        assertFalse(placed.canTransitionTo(OrderStatus.DELIVERED), "PLACED cannot jump directly to DELIVERED");
    }

    @Test
    @DisplayName("Valid progression: ACCEPTED -> PREPARING -> READY_FOR_PICKUP -> OUT_FOR_DELIVERY -> DELIVERED")
    void testHappyPathProgression() {
        OrderStatus status = OrderStatus.ACCEPTED;
        assertTrue(status.canTransitionTo(OrderStatus.PREPARING));
        assertTrue(status.canTransitionTo(OrderStatus.CANCELLED));

        status = OrderStatus.PREPARING;
        assertTrue(status.canTransitionTo(OrderStatus.READY_FOR_PICKUP));
        assertFalse(status.canTransitionTo(OrderStatus.DELIVERED));

        status = OrderStatus.READY_FOR_PICKUP;
        assertTrue(status.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY));

        status = OrderStatus.OUT_FOR_DELIVERY;
        assertTrue(status.canTransitionTo(OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Terminal states have no valid subsequent transitions")
    void testTerminalStates() {
        assertTrue(OrderStatus.DELIVERED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.isTerminal());
        assertTrue(OrderStatus.REJECTED.isTerminal());

        assertFalse(OrderStatus.PLACED.isTerminal());
        assertFalse(OrderStatus.ACCEPTED.isTerminal());
        assertFalse(OrderStatus.PREPARING.isTerminal());

        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(target));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target));
            assertFalse(OrderStatus.REJECTED.canTransitionTo(target));
        }
    }

    @Test
    @DisplayName("Lookup from DB string value")
    void testFromDbValue() {
        assertEquals(OrderStatus.PLACED, OrderStatus.fromDbValue("placed"));
        assertEquals(OrderStatus.READY_FOR_PICKUP, OrderStatus.fromDbValue("ready_for_pickup"));
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.fromDbValue("OUT_FOR_DELIVERY"));
        assertThrows(IllegalArgumentException.class, () -> OrderStatus.fromDbValue("unknown_status"));
    }
}
