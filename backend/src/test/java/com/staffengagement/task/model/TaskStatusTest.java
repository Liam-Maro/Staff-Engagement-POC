package com.staffengagement.task.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskStatusTest {

    // --- getDisplayName ---

    @Test
    void todo_hasDisplayName_ToDo() {
        assertThat(TaskStatus.TODO.getDisplayName()).isEqualTo("To Do");
    }

    @Test
    void inProgress_hasDisplayName_InProgress() {
        assertThat(TaskStatus.IN_PROGRESS.getDisplayName()).isEqualTo("In Progress");
    }

    @Test
    void done_hasDisplayName_Done() {
        assertThat(TaskStatus.DONE.getDisplayName()).isEqualTo("Done");
    }

    // --- fromValue with enum names ---

    @Test
    void fromValue_acceptsEnumName_TODO() {
        assertThat(TaskStatus.fromValue("TODO")).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void fromValue_acceptsEnumName_IN_PROGRESS() {
        assertThat(TaskStatus.fromValue("IN_PROGRESS")).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void fromValue_acceptsEnumName_DONE() {
        assertThat(TaskStatus.fromValue("DONE")).isEqualTo(TaskStatus.DONE);
    }

    // --- fromValue with display names ---

    @Test
    void fromValue_acceptsDisplayName_ToDo() {
        assertThat(TaskStatus.fromValue("To Do")).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void fromValue_acceptsDisplayName_InProgress() {
        assertThat(TaskStatus.fromValue("In Progress")).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void fromValue_acceptsDisplayName_Done() {
        assertThat(TaskStatus.fromValue("Done")).isEqualTo(TaskStatus.DONE);
    }

    // --- fromValue case-insensitive ---

    @ParameterizedTest
    @ValueSource(strings = {"todo", "Todo", "tOdO", "TODO"})
    void fromValue_isCaseInsensitive_forEnumName(String input) {
        assertThat(TaskStatus.fromValue(input)).isEqualTo(TaskStatus.TODO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"to do", "TO DO", "To do", "to Do"})
    void fromValue_isCaseInsensitive_forDisplayName(String input) {
        assertThat(TaskStatus.fromValue(input)).isEqualTo(TaskStatus.TODO);
    }

    // --- fromValue invalid values ---

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "OPEN", "COMPLETED", "PENDING", "", "null"})
    void fromValue_throwsForInvalidValues(String input) {
        assertThatThrownBy(() -> TaskStatus.fromValue(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid task status");
    }
}
