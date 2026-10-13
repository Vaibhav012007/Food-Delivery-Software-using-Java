package com.app.concurrency;

import com.app.service.DeliveryBatchingService;
import com.app.service.SurplusService;
import com.app.service.SurplusServiceImpl;
import com.app.utility.DBConnection;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Application lifecycle listener.
 * 
 * Responsibilities:
 * 1. Initializes the HikariCP database connection pool.
 * 2. Starts the notification queue worker thread.
 * 3. Schedules the delivery batching service to run periodically.
 * 4. Schedules the surplus listing expiry task.
 * 5. Cleanly shuts down all executors and the pool on undeploy.
 * 
 * Threading model:
 * - Notification worker: single thread from a dedicated executor.
 * - Batching: ScheduledExecutorService, 60-second interval.
 * - Surplus expiry: ScheduledExecutorService, 300-second interval.
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    private ScheduledFuture<?> batchingFuture;
    private ScheduledFuture<?> surplusExpiryFuture;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[AppLifecycle] Starting FoodXpress application...");

        // 1. Initialize DB pool
        try {
            DBConnection.init();
        } catch (Exception e) {
            System.err.println("[AppLifecycle] DB pool initialization failed: " + e.getMessage());
            // Don't abort startup; requests that need DB will fail with a clear error
        }

        // 2. Initialize executor pool
        AppExecutors.init();

        // 3. Start notification worker
        NotificationService.getInstance().start();

        // 4. Schedule delivery batching (runs every 60 seconds, initial delay 30 seconds)
        DeliveryBatchingService batchingService = new DeliveryBatchingService();
        batchingFuture = AppExecutors.getScheduler().scheduleWithFixedDelay(
            () -> {
                try {
                    int batched = batchingService.runBatchingCycle();
                    if (batched > 0) {
                        System.out.println("[Batching] Cycle complete. Orders batched: " + batched);
                    }
                } catch (Exception e) {
                    // Catch ALL exceptions to prevent the scheduler from dying
                    System.err.println("[Batching] Cycle error: " + e.getMessage());
                    e.printStackTrace();
                }
            },
            30, 60, TimeUnit.SECONDS
        );

        // 5. Schedule surplus expiry check (every 5 minutes, initial delay 60 seconds)
        SurplusService surplusService = new SurplusServiceImpl();
        surplusExpiryFuture = AppExecutors.getScheduler().scheduleWithFixedDelay(
            () -> {
                try {
                    int expired = surplusService.expireListings();
                    if (expired > 0) {
                        System.out.println("[Surplus] Expired " + expired + " listing(s).");
                    }
                } catch (Exception e) {
                    System.err.println("[Surplus] Expiry check error: " + e.getMessage());
                    e.printStackTrace();
                }
            },
            60, 300, TimeUnit.SECONDS
        );

        System.out.println("[AppLifecycle] FoodXpress started. DB pool, executors, and schedulers are ready.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[AppLifecycle] Shutting down FoodXpress...");

        // Cancel scheduled tasks
        if (batchingFuture != null) batchingFuture.cancel(false);
        if (surplusExpiryFuture != null) surplusExpiryFuture.cancel(false);

        // Stop notification worker
        NotificationService.getInstance().shutdown();

        // Shutdown executors
        AppExecutors.shutdown();

        // Close DB pool last
        DBConnection.closePool();

        System.out.println("[AppLifecycle] FoodXpress shutdown complete.");
    }
}
