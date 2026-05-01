package com.example.sakeec.exception;

public class InvalidStatusTransitionException extends BusinessException {

    private final String currentStatus;

    public InvalidStatusTransitionException(String from, String to, String currentStatus) {
        super("INVALID_STATUS_TRANSITION", "Cannot change status from " + from + " to " + to);
        this.currentStatus = currentStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
