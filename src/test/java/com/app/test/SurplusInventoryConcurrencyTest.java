package com.app.test;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Surplus Inventory Concurrency Simulation Tests")
public class SurplusInventoryConcurrencyTest {

    /**
     * Simulates the database atomic CAS (compare-and-swap) pattern implemented in:
     * UPDATE surplus_listings SET available_quantity = available_quantity - ?
     * WHERE listing_id = ? AND available_quantity >= ? AND status = 'active'
     */
    static class AtomicInventory {
        private final AtomicInteger available;

        public AtomicInventory(int initialStock) {
            this.available = new AtomicInteger(initialStock);
        }

        public boolean tryReserve(int quantity) {
            while (true) {
                int current = available.get();
                if (current < quantity) {
                    return false; // Insufficient stock
                }
                if (available.compareAndSet(current, current - quantity)) {
                    return true; // Successfully decremented
                }
            }
        }

        public int getAvailable() {
            return available.get();
        }
    }

    @Test
    @DisplayName("100 concurrent requests competing for 15 units of surplus stock (never oversell)")
    void testConcurrentSurplusStockDeduction() throws InterruptedException {
        final int initialStock = 15;
        final int requesterCount = 100;
        final AtomicInventory inventory = new AtomicInventory(initialStock);

        final ExecutorService executor = Executors.newFixedThreadPool(requesterCount);
        final CountDownLatch readyLatch = new CountDownLatch(requesterCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final AtomicInteger successfulReservations = new AtomicInteger(0);
        final AtomicInteger rejectedReservations = new AtomicInteger(0);

        for (int i = 0; i < requesterCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    if (inventory.tryReserve(1)) {
                        successfulReservations.incrementAndGet();
                    } else {
                        rejectedReservations.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        assertTrue(readyLatch.await(5, TimeUnit.SECONDS));
        startLatch.countDown();

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));

        // Invariant 1: Exactly 15 reservations succeeded
        assertEquals(initialStock, successfulReservations.get(), "Must reserve exactly initialStock items");
        // Invariant 2: 85 reservations were rejected due to insufficient stock
        assertEquals(requesterCount - initialStock, rejectedReservations.get(), "Remaining requests must be rejected");
        // Invariant 3: Remaining stock is exactly 0, never negative
        assertEquals(0, inventory.getAvailable(), "Inventory must never drop below zero");
    }
}
