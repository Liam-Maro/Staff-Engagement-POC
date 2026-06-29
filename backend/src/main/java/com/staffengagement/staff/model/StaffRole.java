package com.staffengagement.staff.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum StaffRole {
    STAFF("Staff"),
    ADMIN("Admin");

    private final String displayName;

    StaffRole(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static StaffRole fromValue(String value) {
        for (StaffRole role : values()) {
            if (role.name().equalsIgnoreCase(value) || role.displayName.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid staff role: " + value);
    }
}
