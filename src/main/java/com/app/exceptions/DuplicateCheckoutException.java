package com.app.exceptions;

/**
 * Thrown when a duplicate checkout attempt is detected via idempotency key.
 * The existing order ID is returned so the client can redirect to it.
 */
public class DuplicateCheckoutException extends AppException {
    private final int existingOrderId;
    
    public DuplicateCheckoutException(int existingOrderId) {
        super("Duplicate checkout detected. Order already placed with ID: " + existingOrderId, 409);
        this.existingOrderId = existingOrderId;
    }
    
    public int getExistingOrderId() { return existingOrderId; }
}
