package com.app.concurrency;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-group-order checkout lock guard.
 * 
 * Prevents two concurrent requests from finalizing the same group order twice.
 * 
 * This in-memory lock is the FIRST line of defense.
 * The database-level idempotency key (checkout_idempotency_key UNIQUE constraint)
 * is the SECOND, persistent defense for multi-instance deployments.
 * 
 * Usage:
 *   boolean locked = GroupCheckoutGuard.tryLock(groupId);
 *   if (!locked) throw new DuplicateCheckoutException(...);
 *   try {
 *       // perform checkout
 *   } finally {
 *       GroupCheckoutGuard.unlock(groupId);
 *   }
 */
public final class GroupCheckoutGuard {

    // Maps groupOrderId -> a sentinel object used as a lock token
    // ConcurrentHashMap.putIfAbsent guarantees exactly one winner
    private static final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();
    private static final Object SENTINEL = new Object();

    /**
     * Attempts to acquire the checkout lock for the given group order.
     *
     * @param groupOrderId the group order being checked out
     * @return true if the lock was acquired; false if another thread already holds it
     */
    public static boolean tryLock(int groupOrderId) {
        return locks.putIfAbsent(groupOrderId, SENTINEL) == null;
    }

    /**
     * Releases the checkout lock for the given group order.
     * Must be called in a finally block after tryLock() returns true.
     */
    public static void unlock(int groupOrderId) {
        locks.remove(groupOrderId);
    }

    /**
     * Returns whether the given group order is currently being checked out.
     * (For monitoring/debugging only.)
     */
    public static boolean isLocked(int groupOrderId) {
        return locks.containsKey(groupOrderId);
    }

    private GroupCheckoutGuard() { /* utility class */ }
}
