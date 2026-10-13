package com.app.exceptions;

/**
 * Thrown when an invalid order state transition is attempted.
 * Enforces the order state machine: PLACED -> ACCEPTED -> PREPARING -> READY_FOR_PICKUP -> OUT_FOR_DELIVERY -> DELIVERED.
 */
public class InvalidOrderStateException extends AppException {
    private final String currentStatus;
    private final String requestedStatus;
    
    public InvalidOrderStateException(int orderId, String currentStatus, String requestedStatus) {
        super("Invalid state transition for order " + orderId + 
              ". Cannot move from '" + currentStatus + "' to '" + requestedStatus + "'", 409);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }
    
    public String getCurrentStatus() { return currentStatus; }
    public String getRequestedStatus() { return requestedStatus; }
}
