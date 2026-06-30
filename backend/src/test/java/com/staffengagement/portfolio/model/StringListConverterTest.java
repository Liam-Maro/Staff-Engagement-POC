package com.staffengagement.portfolio.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StringListConverterTest {

    private StringListConverter converter;

    @BeforeEach
    void setUp() {
        converter = new StringListConverter();
    }

    @Test
    void convertToDatabaseColumn_shouldReturnEmptyArray_whenNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
    }

    @Test
    void convertToDatabaseColumn_shouldReturnEmptyArray_whenEmpty() {
        assertThat(converter.convertToDatabaseColumn(Collections.emptyList())).isEqualTo("[]");
    }

    @Test
    void convertToDatabaseColumn_shouldReturnJsonArray() {
        String result = converter.convertToDatabaseColumn(List.of("Java", "Spring"));
        assertThat(result).isEqualTo("[\"Java\",\"Spring\"]");
    }

    @Test
    void convertToEntityAttribute_shouldReturnEmptyList_whenNull() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    void convertToEntityAttribute_shouldReturnEmptyList_whenBlank() {
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
        assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
    }

    @Test
    void convertToEntityAttribute_shouldReturnList_fromJsonArray() {
        List<String> result = converter.convertToEntityAttribute("[\"Go\",\"Rust\"]");
        assertThat(result).containsExactly("Go", "Rust");
    }

    @Test
    void convertToEntityAttribute_shouldThrow_whenInvalidJson() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("not json"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
