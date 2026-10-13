package com.app.concurrency;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Asynchronous notification service.
 * 
 * Uses a single consumer thread reading from a LinkedBlockingQueue.
 * Notifications are processed independently of the request/response cycle,
 * so a notification failure CANNOT affect order placement.
 * 
 * In production this would send SMS/push/email.
 * Here it simulates notifications with a console log and persists a record.
 */
public class NotificationService {

    /** Simple notification job DTO */
    public static class NotificationJob {
        public final int userId;
        public final String message;
        public final Integer orderId; // nullable

        public NotificationJob(int userId, String message, Integer orderId) {
            this.userId = userId;
            this.message = message;
            this.orderId = orderId;
        }
    }

    private static final NotificationService INSTANCE = new NotificationService();
    private final LinkedBlockingQueue<NotificationJob> queue = new LinkedBlockingQueue<>(1000);
    private volatile boolean running = false;
    private Thread consumerThread;

    private NotificationService() {}

    public static NotificationService getInstance() {
        return INSTANCE;
    }

    /**
     * Starts the consumer thread. Called once at startup.
     */
    public void start() {
        if (running) return;
        running = true;
        consumerThread = new Thread(this::consumeLoop, "fx-notification-worker");
        consumerThread.setDaemon(true);
        consumerThread.start();
        System.out.println("[NotificationService] Consumer thread started.");
    }

    /**
     * Shuts down the consumer thread gracefully.
     */
    public void shutdown() {
        running = false;
        if (consumerThread != null) {
            consumerThread.interrupt();
        }
        System.out.println("[NotificationService] Shutdown requested.");
    }

    /**
     * Submits a notification job asynchronously.
     * This method is non-blocking; the caller does not wait for delivery.
     * A failure to enqueue (queue full) is logged but does not throw.
     */
    public void submit(NotificationJob job) {
        boolean added = queue.offer(job);
        if (!added) {
            System.err.println("[NotificationService] Queue full! Notification dropped for user " + job.userId);
        }
    }

    /**
     * Convenience method to submit a notification.
     */
    public void notify(int userId, String message, Integer orderId) {
        submit(new NotificationJob(userId, message, orderId));
    }

    /**
     * Consumer loop running on the worker thread.
     * Polls the queue with a 1-second timeout to allow clean shutdown.
     */
    private void consumeLoop() {
        System.out.println("[NotificationService] Consumer loop running.");
        while (running || !queue.isEmpty()) {
            try {
                NotificationJob job = queue.poll(1, TimeUnit.SECONDS);
                if (job != null) {
                    processNotification(job);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Catch all to keep the consumer alive
                System.err.println("[NotificationService] Error processing notification: " + e.getMessage());
            }
        }
        System.out.println("[NotificationService] Consumer loop exiting.");
    }

    /**
     * Processes a single notification.
     * In production: send email/push/SMS via external service.
     * Here: logs to console (simulated) and persists to notification_log via DB.
     */
    private void processNotification(NotificationJob job) {
        // Simulated notification delivery
        System.out.println("[NOTIFICATION] userId=" + job.userId +
            (job.orderId != null ? ", orderId=" + job.orderId : "") +
            ", msg=" + job.message);
        
        // Persist to notification_log table
        try (java.sql.Connection conn = com.app.utility.DBConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(
                 "INSERT INTO notification_log (order_id, user_id, message, status, sent_at) " +
                 "VALUES (?, ?, ?, 'sent', NOW())")) {
            if (job.orderId != null) {
                ps.setInt(1, job.orderId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setInt(2, job.userId);
            ps.setString(3, job.message);
            ps.executeUpdate();
        } catch (Exception e) {
            // Notification persistence failure must NOT affect the caller
            System.err.println("[NotificationService] Failed to persist notification log: " + e.getMessage());
        }
    }
}
