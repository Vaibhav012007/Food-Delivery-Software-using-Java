package com.app.exceptions;

/**
 * Thrown when an operation is attempted on a group order that has expired or is closed.
 */
public class GroupExpiredException extends AppException {
    private final int groupOrderId;
    
    public GroupExpiredException(int groupOrderId, String status) {
        super("Group order " + groupOrderId + " is not open. Status: " + status, 409);
        this.groupOrderId = groupOrderId;
    }
    
    public int getGroupOrderId() { return groupOrderId; }
}
