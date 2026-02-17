package com.erp.ia.execution;

/**
 * Result of executing an action.
 */
public record ExecutionResult(
        String status, // SUCCESS or FAILED
        String actionType,
        String message,
        Object output) {
    public static ExecutionResult success(String actionType, String message, Object output) {
        return new ExecutionResult("SUCCESS", actionType, message, output);
    }

    public static ExecutionResult failed(String actionType, String message) {
        return new ExecutionResult("FAILED", actionType, message, null);
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(status);
    }
}
