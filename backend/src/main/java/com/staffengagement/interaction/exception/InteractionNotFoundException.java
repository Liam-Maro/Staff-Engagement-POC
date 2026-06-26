package com.staffengagement.interaction.exception;

import java.util.UUID;

public class InteractionNotFoundException extends RuntimeException {
    public InteractionNotFoundException(UUID id) {
        super("Interaction not found: " + id);
    }
}
