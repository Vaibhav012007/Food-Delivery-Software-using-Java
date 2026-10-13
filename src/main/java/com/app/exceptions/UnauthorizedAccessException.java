package com.app.exceptions;

/**
 * Thrown when a user attempts an operation they are not authorized to perform.
 */
public class UnauthorizedAccessException extends AppException {
    public UnauthorizedAccessException(String message) {
        super(message, 403);
    }
    
    public UnauthorizedAccessException(int userId, String action) {
        super("User " + userId + " is not authorized to: " + action, 403);
    }
}
