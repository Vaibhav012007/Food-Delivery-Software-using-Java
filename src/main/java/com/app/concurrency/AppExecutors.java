package com.app.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages shared thread pools for the FoodXpress application.
 * 
 * Two pools:
 * 1. scheduler: ScheduledExecutorService (2 threads) for periodic background tasks
 *    (delivery batching, surplus expiry).
 * 2. workerPool: ExecutorService (4 threads) for concurrent order processing
 *    and other async operations.
 * 
 * Thread safety: init() and shutdown() are called once each by AppLifecycleListener
 * on the servlet container thread. getScheduler() and getWorkerPool() are safe
 * to call from any thread after init().
 */
public final class AppExecutors {

    private static ScheduledExecutorService scheduler;
    private static ExecutorService workerPool;

    /**
     * Initializes the thread pools. Must be called once at application startup.
     */
    public static synchronized void init() {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "fx-scheduler-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
        }
        if (workerPool == null) {
            workerPool = Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "fx-worker-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
        }
        System.out.println("[AppExecutors] Thread pools initialized (scheduler=2, workers=4).");
    }

    /**
     * Returns the scheduler for periodic background tasks.
     */
    public static ScheduledExecutorService getScheduler() {
        if (scheduler == null) init();
        return scheduler;
    }

    /**
     * Returns the general-purpose worker pool for async order processing.
     */
    public static ExecutorService getWorkerPool() {
        if (workerPool == null) init();
        return workerPool;
    }

    /**
     * Shuts down both pools gracefully. Waits up to 10 seconds for tasks to finish.
     * Called by AppLifecycleListener.contextDestroyed().
     */
    public static synchronized void shutdown() {
        shutdownPool("scheduler", scheduler);
        shutdownPool("workerPool", workerPool);
        scheduler = null;
        workerPool = null;
    }

    private static void shutdownPool(String name, ExecutorService pool) {
        if (pool == null || pool.isShutdown()) return;
        pool.shutdown();
        try {
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow();
                System.err.println("[AppExecutors] " + name + " did not terminate cleanly; forced shutdown.");
            } else {
                System.out.println("[AppExecutors] " + name + " terminated cleanly.");
            }
        } catch (InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private AppExecutors() { /* utility class */ }
}
