package com.staffengagement.interaction.exception;

public class InvalidDateRangeException extends RuntimeException {
    public InvalidDateRangeException() {
        super("fromDate must be on or before toDate");
    }
}
