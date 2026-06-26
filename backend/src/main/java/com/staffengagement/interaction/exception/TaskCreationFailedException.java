package com.staffengagement.interaction.exception;

public class TaskCreationFailedException extends RuntimeException {
    public TaskCreationFailedException(String cause) {
        super("Failed to create follow-up task: " + cause);
    }
}
