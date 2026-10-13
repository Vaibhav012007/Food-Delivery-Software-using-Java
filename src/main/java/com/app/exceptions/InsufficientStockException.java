package com.app.exceptions;

/**
 * Thrown when a purchase or reservation would reduce stock below zero.
 * Protects against overselling in concurrent scenarios.
 */
public class InsufficientStockException extends AppException {
    private final int menuItemId;
    private final int requestedQuantity;
    private final int availableQuantity;
    
    public InsufficientStockException(int menuItemId, int requestedQuantity, int availableQuantity) {
        super("Insufficient stock for menu item " + menuItemId + 
              ". Requested: " + requestedQuantity + ", Available: " + availableQuantity, 409);
        this.menuItemId = menuItemId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }
    
    public int getMenuItemId() { return menuItemId; }
    public int getRequestedQuantity() { return requestedQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
}
