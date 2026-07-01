package com.staffengagement.staff.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StaffRoleTest {

    @Test
    void staff_hasDisplayName() {
        assertThat(StaffRole.STAFF.getDisplayName()).isEqualTo("Staff");
    }

    @Test
    void admin_hasDisplayName() {
        assertThat(StaffRole.ADMIN.getDisplayName()).isEqualTo("Admin");
    }

    @Test
    void fromValue_acceptsEnumName() {
        assertThat(StaffRole.fromValue("STAFF")).isEqualTo(StaffRole.STAFF);
        assertThat(StaffRole.fromValue("ADMIN")).isEqualTo(StaffRole.ADMIN);
    }

    @Test
    void fromValue_acceptsDisplayName() {
        assertThat(StaffRole.fromValue("Staff")).isEqualTo(StaffRole.STAFF);
        assertThat(StaffRole.fromValue("Admin")).isEqualTo(StaffRole.ADMIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "STAFF", "Staff", "admin", "ADMIN", "Admin"})
    void fromValue_isCaseInsensitive(String input) {
        assertThat(StaffRole.fromValue(input)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "MANAGER", "SUPERADMIN", ""})
    void fromValue_throwsForInvalidValues(String input) {
        assertThatThrownBy(() -> StaffRole.fromValue(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid staff role");
    }
}
