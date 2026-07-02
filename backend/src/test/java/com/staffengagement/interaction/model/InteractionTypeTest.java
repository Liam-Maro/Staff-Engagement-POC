package com.staffengagement.interaction.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InteractionTypeTest {

    @Test
    void checkIn_hasDisplayName() {
        assertThat(InteractionType.CHECK_IN.getDisplayName()).isEqualTo("Check-in");
    }

    @Test
    void mentoring_hasDisplayName() {
        assertThat(InteractionType.MENTORING.getDisplayName()).isEqualTo("Mentoring");
    }

    @Test
    void catchUp_hasDisplayName() {
        assertThat(InteractionType.CATCH_UP.getDisplayName()).isEqualTo("Catch-up");
    }

    @Test
    void performanceReview_hasDisplayName() {
        assertThat(InteractionType.PERFORMANCE_REVIEW.getDisplayName()).isEqualTo("Performance Review");
    }

    @Test
    void informal_hasDisplayName() {
        assertThat(InteractionType.INFORMAL.getDisplayName()).isEqualTo("Informal");
    }

    @Test
    void fromValue_acceptsEnumName() {
        assertThat(InteractionType.fromValue("CHECK_IN")).isEqualTo(InteractionType.CHECK_IN);
        assertThat(InteractionType.fromValue("MENTORING")).isEqualTo(InteractionType.MENTORING);
        assertThat(InteractionType.fromValue("CATCH_UP")).isEqualTo(InteractionType.CATCH_UP);
    }

    @Test
    void fromValue_acceptsDisplayName() {
        assertThat(InteractionType.fromValue("Check-in")).isEqualTo(InteractionType.CHECK_IN);
        assertThat(InteractionType.fromValue("Mentoring")).isEqualTo(InteractionType.MENTORING);
        assertThat(InteractionType.fromValue("Catch-up")).isEqualTo(InteractionType.CATCH_UP);
        assertThat(InteractionType.fromValue("Performance Review")).isEqualTo(InteractionType.PERFORMANCE_REVIEW);
        assertThat(InteractionType.fromValue("Informal")).isEqualTo(InteractionType.INFORMAL);
    }

    @ParameterizedTest
    @ValueSource(strings = {"check_in", "Check_In", "mentoring", "MENTORING"})
    void fromValue_isCaseInsensitive(String input) {
        assertThat(InteractionType.fromValue(input)).isNotNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "Meeting", "CALL", ""})
    void fromValue_throwsForInvalidValues(String input) {
        assertThatThrownBy(() -> InteractionType.fromValue(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid interaction type");
    }
}
