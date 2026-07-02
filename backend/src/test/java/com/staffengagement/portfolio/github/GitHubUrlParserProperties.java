package com.staffengagement.portfolio.github;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Property-based tests for {@link GitHubUrlParser}.
 */
class GitHubUrlParserProperties {

    // ========================================================================
    // Feature: github-profile-import, Property 1: URL Parsing Round-Trip
    // ========================================================================

    /**
     * Property 1: URL Parsing Round-Trip
     *
     * For any valid GitHub username (1–39 alphanumeric/hyphen chars, no leading/trailing/consecutive
     * hyphens), constructing {@code https://github.com/{username}} and parsing it SHALL produce
     * the same username.
     *
     * **Validates: Requirements 1.1, 1.2**
     */
    @Property(tries = 100)
    void urlParsingRoundTrip(@ForAll("validGitHubUsernames") String username) {
        String url = "https://github.com/" + username;

        GitHubUrlParser.ParseResult result = GitHubUrlParser.parse(url);

        assertThat(result.username()).isEqualTo(username);
    }

    @Provide
    Arbitrary<String> validGitHubUsernames() {
        // GitHub usernames: 1–39 chars, alphanumeric + hyphens,
        // no leading/trailing/consecutive hyphens
        Arbitrary<String> alphanumOnly = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(39);

        Arbitrary<String> withHyphens = Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('A', 'Z')
                .withCharRange('0', '9')
                .withChars('-')
                .ofMinLength(1)
                .ofMaxLength(39)
                .filter(s -> !s.startsWith("-"))
                .filter(s -> !s.endsWith("-"))
                .filter(s -> !s.contains("--"));

        return Arbitraries.oneOf(alphanumOnly, withHyphens);
    }

    // ========================================================================
    // Feature: github-profile-import, Property 2: Invalid URLs Are Rejected
    // ========================================================================

    /**
     * URLs with wrong domains (not github.com) must be rejected.
     *
     * **Validates: Requirements 1.3, 1.6**
     */
    @Property(tries = 100)
    void wrongDomainUrlsAreRejected(@ForAll("wrongDomainUrls") String url) {
        assertThatThrownBy(() -> GitHubUrlParser.parse(url))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    /**
     * URLs with extra path segments beyond the username are rejected.
     *
     * **Validates: Requirements 1.3, 1.6**
     */
    @Property(tries = 100)
    void extraPathSegmentUrlsAreRejected(@ForAll("extraSegmentUrls") String url) {
        assertThatThrownBy(() -> GitHubUrlParser.parse(url))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    /**
     * URLs with query parameters are rejected.
     *
     * **Validates: Requirements 1.3, 1.6**
     */
    @Property(tries = 100)
    void queryParamUrlsAreRejected(@ForAll("queryParamUrls") String url) {
        assertThatThrownBy(() -> GitHubUrlParser.parse(url))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    /**
     * URLs with fragment identifiers are rejected.
     *
     * **Validates: Requirements 1.3, 1.6**
     */
    @Property(tries = 100)
    void fragmentUrlsAreRejected(@ForAll("fragmentUrls") String url) {
        assertThatThrownBy(() -> GitHubUrlParser.parse(url))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    /**
     * URLs with invalid username characters are rejected.
     *
     * **Validates: Requirements 1.3, 1.6**
     */
    @Property(tries = 100)
    void invalidUsernameCharsAreRejected(@ForAll("invalidUsernameUrls") String url) {
        assertThatThrownBy(() -> GitHubUrlParser.parse(url))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    /**
     * Null and blank strings are rejected.
     *
     * **Validates: Requirements 1.3, 1.6**
     */
    @Property(tries = 100)
    void nullAndBlankStringsAreRejected(@ForAll("nullOrBlankStrings") String url) {
        assertThatThrownBy(() -> GitHubUrlParser.parse(url))
                .isInstanceOf(InvalidGitHubUrlException.class);
    }

    // ========================================================================
    // Providers
    // ========================================================================

    @Provide
    Arbitrary<String> wrongDomainUrls() {
        Arbitrary<String> domains = Arbitraries.of(
                "gitlab.com", "bitbucket.org", "sourceforge.net",
                "codeberg.org", "sr.ht", "example.com",
                "github.io", "githubz.com", "notgithub.com"
        );
        Arbitrary<String> usernames = validUsernames();
        return Combinators.combine(domains, usernames)
                .as((domain, username) -> "https://" + domain + "/" + username);
    }

    @Provide
    Arbitrary<String> extraSegmentUrls() {
        Arbitrary<String> usernames = validUsernames();
        Arbitrary<String> extraSegments = Arbitraries.of(
                "repo", "settings", "stars", "repositories",
                "followers", "following", "packages"
        );
        return Combinators.combine(usernames, extraSegments)
                .as((username, segment) -> "https://github.com/" + username + "/" + segment);
    }

    @Provide
    Arbitrary<String> queryParamUrls() {
        Arbitrary<String> usernames = validUsernames();
        Arbitrary<String> params = Arbitraries.of(
                "?tab=repos", "?tab=stars", "?page=2",
                "?sort=name", "?q=java", "?tab=repositories"
        );
        return Combinators.combine(usernames, params)
                .as((username, param) -> "https://github.com/" + username + param);
    }

    @Provide
    Arbitrary<String> fragmentUrls() {
        Arbitrary<String> usernames = validUsernames();
        Arbitrary<String> fragments = Arbitraries.of(
                "#section", "#repos", "#about", "#top",
                "#contributions", "#readme"
        );
        return Combinators.combine(usernames, fragments)
                .as((username, fragment) -> "https://github.com/" + username + fragment);
    }

    @Provide
    Arbitrary<String> invalidUsernameUrls() {
        Arbitrary<String> invalidUsernames = Arbitraries.oneOf(
                // Leading hyphen
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .map(s -> "-" + s),
                // Trailing hyphen
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(10)
                        .map(s -> s + "-"),
                // Consecutive hyphens
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(5)
                        .flatMap(prefix -> Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(5)
                                .map(suffix -> prefix + "--" + suffix)),
                // Username too long (40+ chars)
                Arbitraries.strings().withCharRange('a', 'z').ofMinLength(40).ofMaxLength(50),
                // Contains invalid characters
                Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(5)
                        .flatMap(prefix -> Arbitraries.of("_", ".", "@", "!", " ", "$", "%", "+")
                                .flatMap(badChar -> Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(5)
                                        .map(suffix -> prefix + badChar + suffix)))
        );
        return invalidUsernames.map(u -> "https://github.com/" + u);
    }

    @Provide
    Arbitrary<String> nullOrBlankStrings() {
        return Arbitraries.oneOf(
                Arbitraries.just(null),
                Arbitraries.just(""),
                Arbitraries.just("   "),
                Arbitraries.just("\t"),
                Arbitraries.just("\n"),
                Arbitraries.just("  \t\n  ")
        );
    }

    // Helper: generate valid usernames for composing invalid URLs
    private Arbitrary<String> validUsernames() {
        return Arbitraries.strings()
                .withCharRange('a', 'z')
                .withCharRange('0', '9')
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.isEmpty()
                        && !s.startsWith("-")
                        && !s.endsWith("-")
                        && !s.contains("--")
                        && s.matches("[a-z0-9]+"));
    }

    // ========================================================================
    // Feature: github-profile-import, Property 3: URL Normalization Preserves Semantics
    // ========================================================================

    /**
     * Property 3: URL Normalization Preserves Semantics
     *
     * For any valid GitHub profile URL with added leading/trailing whitespace or a trailing slash,
     * parsing SHALL produce the same username as the canonical form without whitespace or trailing slash.
     *
     * **Validates: Requirements 1.5, 1.7**
     */
    @Property(tries = 100)
    void urlNormalizationPreservesSemantics(
            @ForAll("validGitHubUsernames") String username,
            @ForAll("whitespaceVariations") WhitespaceVariation variation) {

        String canonicalUrl = "https://github.com/" + username;
        String modifiedUrl = variation.apply(canonicalUrl);

        GitHubUrlParser.ParseResult canonicalResult = GitHubUrlParser.parse(canonicalUrl);
        GitHubUrlParser.ParseResult modifiedResult = GitHubUrlParser.parse(modifiedUrl);

        assertThat(modifiedResult.username()).isEqualTo(canonicalResult.username());
    }

    record WhitespaceVariation(String leadingSpace, String trailingSlash, String trailingSpace) {
        String apply(String url) {
            return leadingSpace + url + trailingSlash + trailingSpace;
        }
    }

    @Provide
    Arbitrary<WhitespaceVariation> whitespaceVariations() {
        Arbitrary<String> leadingSpaces = Arbitraries.of("", " ", "  ", "\t", " \t ");
        Arbitrary<String> trailingSpaces = Arbitraries.of("", " ", "  ", "\t", " \t ");
        Arbitrary<String> trailingSlashes = Arbitraries.of("", "/", "//", "///");

        return Combinators.combine(leadingSpaces, trailingSlashes, trailingSpaces)
                .as(WhitespaceVariation::new)
                .filter(v -> !v.leadingSpace().isEmpty()
                        || !v.trailingSpace().isEmpty()
                        || !v.trailingSlash().isEmpty());
    }
}
