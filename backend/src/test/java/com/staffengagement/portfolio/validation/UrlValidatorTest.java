package com.staffengagement.portfolio.validation;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class UrlValidatorTest {

    private UrlValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new UrlValidator();
        context = mock(ConstraintValidatorContext.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com",
            "https://github.com/user/repo",
            "http://localhost:8080/path",
            "https://www.linkedin.com/in/username"
    })
    void shouldAcceptValidHttpAndHttpsUrls(String url) {
        assertThat(validator.isValid(url, context)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "ftp://example.com",
            "not-a-url",
            "file:///tmp/file.txt",
            "mailto:user@example.com",
            "://missing-scheme.com"
    })
    void shouldRejectInvalidUrls(String url) {
        assertThat(validator.isValid(url, context)).isFalse();
    }

    @Test
    void shouldRejectNull() {
        assertThat(validator.isValid(null, context)).isFalse();
    }

    @Test
    void shouldRejectBlankString() {
        assertThat(validator.isValid("   ", context)).isFalse();
    }
}
