package com.staffengagement.interaction.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum InteractionType {
    CHECK_IN("Check-in"),
    MENTORING("Mentoring"),
    CATCH_UP("Catch-up"),
    PERFORMANCE_REVIEW("Performance Review"),
    INFORMAL("Informal");

    private final String displayName;

    InteractionType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static InteractionType fromValue(String value) {
        for (InteractionType type : values()) {
            if (type.name().equalsIgnoreCase(value) || type.displayName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid interaction type: " + value);
    }
}
