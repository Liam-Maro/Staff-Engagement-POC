package com.staffengagement.portfolio.github;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubUrlParserTest {

    @Test
    void parse_validUrl_extractsUsername() {
        var result = GitHubUrlParser.parse("https://github.com/octocat");
        assertThat(result.username()).isEqualTo("octocat");
    }

    @Test
    void parse_trailingSlash_extractsUsername() {
        var result = GitHubUrlParser.parse("https://github.com/octocat/");
        assertThat(result.username()).isEqualTo("octocat");
    }

    @Test
    void parse_leadingAndTrailingWhitespace_extractsUsername() {
        var result = GitHubUrlParser.parse("  https://github.com/octocat  ");
        assertThat(result.username()).isEqualTo("octocat");
    }

    @Test
    void parse_caseInsensitiveSchemeAndHost() {
        var result = GitHubUrlParser.parse("HTTPS://GITHUB.COM/octocat");
        assertThat(result.username()).isEqualTo("octocat");
    }

    @Test
    void parse_usernameWithHyphens() {
        var result = GitHubUrlParser.parse("https://github.com/my-user-name");
        assertThat(result.username()).isEqualTo("my-user-name");
    }

    @Test
    void parse_singleCharUsername() {
        var result = GitHubUrlParser.parse("https://github.com/a");
        assertThat(result.username()).isEqualTo("a");
    }

    @Test
    void parse_maxLengthUsername() {
        String username = "a".repeat(39);
        var result = GitHubUrlParser.parse("https://github.com/" + username);
        assertThat(result.username()).isEqualTo(username);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void parse_nullOrBlank_throwsException(String url) {
        assertThatThrownBy(() -> GitHubUrlParser.parse(url))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_wrongDomain_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://gitlab.com/user"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_extraPathSegments_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/user/repo"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_queryParams_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/user?tab=repos"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_fragment_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/user#readme"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_usernameStartsWithHyphen_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/-user"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_usernameEndsWithHyphen_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/user-"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_usernameConsecutiveHyphens_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/user--name"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_usernameTooLong_throwsException() {
        String longUsername = "a".repeat(40);
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/" + longUsername))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_usernameWithInvalidChars_throwsException() {
        assertThatThrownBy(() -> GitHubUrlParser.parse("https://github.com/user_name"))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    @Test
    void parse_httpScheme_accepted() {
        var result = GitHubUrlParser.parse("http://github.com/octocat");
        assertThat(result.username()).isEqualTo("octocat");
    }
}
