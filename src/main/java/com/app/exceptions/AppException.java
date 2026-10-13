package com.app.exceptions;

/**
 * Base runtime exception for all FoodXpress application exceptions.
 * All custom exceptions extend this class to enable unified catch blocks.
 */
public class AppException extends RuntimeException {
    private final int errorCode;
    
    public AppException(String message) {
        super(message);
        this.errorCode = 500;
    }
    
    public AppException(String message, int errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public AppException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 500;
    }
    
    public AppException(String message, int errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public int getErrorCode() {
        return errorCode;
    }
}
