package com.app.test;

import com.app.concurrency.GroupCheckoutGuard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Group Checkout Concurrency Guard Tests")
public class GroupCheckoutGuardTest {

    @Test
    @DisplayName("Single thread can acquire and release lock")
    void testBasicLockUnlock() {
        int groupId = 1001;
        assertFalse(GroupCheckoutGuard.isLocked(groupId));

        boolean acquired = GroupCheckoutGuard.tryLock(groupId);
        assertTrue(acquired, "First attempt must succeed");
        assertTrue(GroupCheckoutGuard.isLocked(groupId));

        boolean secondAttempt = GroupCheckoutGuard.tryLock(groupId);
        assertFalse(secondAttempt, "Second attempt while locked must fail");

        GroupCheckoutGuard.unlock(groupId);
        assertFalse(GroupCheckoutGuard.isLocked(groupId));

        boolean reacquired = GroupCheckoutGuard.tryLock(groupId);
        assertTrue(reacquired, "Re-acquisition after unlock must succeed");
        GroupCheckoutGuard.unlock(groupId);
    }

    @Test
    @DisplayName("High concurrency race condition: 50 threads competing for same group order checkout")
    void testConcurrentCheckoutRaceCondition() throws InterruptedException {
        final int groupId = 2002;
        final int threadCount = 50;
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        final CountDownLatch readyLatch = new CountDownLatch(threadCount);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final AtomicInteger successCount = new AtomicInteger(0);
        final AtomicInteger failureCount = new AtomicInteger(0);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await(); // Guarantee all threads start at the exact same moment
                        if (GroupCheckoutGuard.tryLock(groupId)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            // Wait for all 50 threads to be ready
            assertTrue(readyLatch.await(5, TimeUnit.SECONDS), "Threads must be ready in time");

            // Release all threads simultaneously
            startLatch.countDown();

            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "All tasks must finish");

            // Exactly ONE thread must have won the lock
            assertEquals(1, successCount.get(), "Exactly ONE thread must acquire the checkout lock");
            assertEquals(threadCount - 1, failureCount.get(), "All other threads must be rejected");

        } finally {
            GroupCheckoutGuard.unlock(groupId);
        }
    }
}
