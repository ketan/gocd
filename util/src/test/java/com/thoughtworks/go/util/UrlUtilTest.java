/*
 * Copyright 2019 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class UrlUtilTest {

    @Test
    void shouldEncodeUrl() {
        assertThat(UrlUtil.encodeInUtf8("a%b")).isEqualTo("a%25b");
    }

    @Test
    void shouldEncodeAllPartsInUrl() {
        assertThat(UrlUtil.encodeInUtf8("a%b/c%d")).isEqualTo("a%25b/c%25d");
    }

    @Test
    void shouldKeepPrecedingSlash() {
        assertThat(UrlUtil.encodeInUtf8("/a%b/c%d")).isEqualTo("/a%25b/c%25d");
    }

    @Test
    void shouldKeepTrailingSlash() {
        assertThat(UrlUtil.encodeInUtf8("a%b/c%d/")).isEqualTo("a%25b/c%25d/");
    }

    @Test
    void shouldAppendQueryString() throws Exception {
        assertThat(UrlUtil.urlWithQuery("http://baz.quux", "foo", "bar")).isEqualTo("http://baz.quux?foo=bar");
        assertThat(UrlUtil.urlWithQuery("http://baz.quux?bang=boom&hello=world", "foo", "bar")).isEqualTo("http://baz.quux?bang=boom&hello=world&foo=bar");
        assertThat(UrlUtil.urlWithQuery("http://baz.quux:1000/hello/world?bang=boom", "foo", "bar")).isEqualTo("http://baz.quux:1000/hello/world?bang=boom&foo=bar");
        assertThat(UrlUtil.urlWithQuery("http://baz.quux:1000/hello/world?bang=boom%20bang&quux=bar/baz&sha1=2jmj7l5rSw0yVb%2FvlWAYkK%2FYBwk%3D", "foo", "bar\\baz")).isEqualTo("http://baz.quux:1000/hello/world?bang=boom+bang&quux=bar%2Fbaz&sha1=2jmj7l5rSw0yVb%2FvlWAYkK%2FYBwk%3D&foo=bar%5Cbaz");
        assertThat(UrlUtil.urlWithQuery("http://baz.quux:1000/hello/world?bang=boom#in_hell", "foo", "bar")).isEqualTo("http://baz.quux:1000/hello/world?bang=boom&foo=bar#in_hell");
        assertThat(UrlUtil.urlWithQuery("http://user:loser@baz.quux:1000/hello/world#in_hell", "foo", "bar")).isEqualTo("http://user:loser@baz.quux:1000/hello/world?foo=bar#in_hell");
    }

    @Test
    void shouldGetGivenQueryParamFromUrl() throws Exception {
        String url = "http://localhost:8153?code=123&new_code=xyz";
        assertThat(UrlUtil.getQueryParamFromUrl(url, "code")).isEqualTo("123");
        assertThat(UrlUtil.getQueryParamFromUrl(url, "new_code")).isEqualTo("xyz");
    }

    @Test
    void shouldReturnEmptyStringIfQueryParamIsNotAvailable() throws Exception {
        String url = "http://localhost:8153?code=123&new_code=xyz";
        assertThat(UrlUtil.getQueryParamFromUrl(url, "not_available")).isEqualTo("");
    }

    @Test
    void shouldReturnEmptyStringIfUrlIsInvalid() throws Exception {
        String url = "this is not valid url";
        assertThat(UrlUtil.getQueryParamFromUrl(url, "param")).isEqualTo("");
    }

    @Test
    void concatPathWithBaseUrl() throws Exception {
        assertThat(UrlUtil.concatPath("http://foo", "bar")).isEqualTo("http://foo/bar");
        assertThat(UrlUtil.concatPath("http://foo/", "bar")).isEqualTo("http://foo/bar");
    }

    @ParameterizedTest
    @MethodSource("getTestStrings")
    void shouldReturnNormalizedURL(String expectedUrl, String originalUrl, String expectedUsername, String expectedPassword, String expectedDenormalizedUrl) {
        String normalizedUrl = UrlUtil.urlWithoutCredentials(originalUrl);
        String username = UrlUtil.getUsername(originalUrl);
        String password = UrlUtil.getPassword(originalUrl);


        assertThat(normalizedUrl)
                .isEqualTo(expectedUrl);

        assertThat(username)
                .isEqualTo(expectedUsername);

        assertThat(password)
                .isEqualTo(expectedPassword);

        String urlWithCredentials = UrlUtil.urlWithCredentials(normalizedUrl, username, password);

        assertThat(urlWithCredentials)
                .isEqualTo(expectedDenormalizedUrl);
    }

    private static Stream<Arguments> getTestStrings() {
        return Stream.of(
                Arguments.of("https://example.com/xxx", "https://example.com/xxx", null, null, "https://example.com/xxx")
                , Arguments.of("https://example.com:8443/xxx", "https://example.com:8443/xxx", null, null, "https://example.com:8443/xxx")

                , Arguments.of("https://example.com/yyy", "https://foo@example.com/yyy", "foo", null, "https://foo@example.com/yyy")
                , Arguments.of("https://example.com/yyy", "https://foo:@example.com/yyy", "foo", null, "https://foo@example.com/yyy")
                , Arguments.of("https://example.com/yyy", "https://:@example.com/yyy", null, null, "https://example.com/yyy")

                , Arguments.of("https://example.com/yyy", "https://:bar@example.com/yyy", null, "bar", "https://:bar@example.com/yyy")
                , Arguments.of("https://example.com/yyy", "https://foo:bar@example.com/yyy", "foo", "bar", "https://foo:bar@example.com/yyy")
                , Arguments.of("https://example.com:8154/aaa", "https://foo@example.com:8154/aaa", "foo", null, "https://foo@example.com:8154/aaa")
                , Arguments.of("https://example.com:8154/vbb", "https://foo:@example.com:8154/vbb", "foo", null, "https://foo@example.com:8154/vbb")
                , Arguments.of("https://example.com:8154/ccc", "https://:bar@example.com:8154/ccc", null, "bar", "https://:bar@example.com:8154/ccc")
                , Arguments.of("https://example.com:8154/eee", "https://foo:bar@example.com:8154/eee", "foo", "bar", "https://foo:bar@example.com:8154/eee")
                , Arguments.of("https://github.com/gocd/gocd", "https://bobfoo%40example.com:p%40ssw0r&:d@github.com/gocd/gocd", "bobfoo@example.com", "p@ssw0r&:d", "https://bobfoo%40example.com:p%40ssw0r&:d@github.com/gocd/gocd")
                , Arguments.of("http://github.com/gocd/gocd", "http://bobfoo%40example.com:p%40ssw0r&:d@github.com/gocd/gocd", "bobfoo@example.com", "p@ssw0r&:d", "http://bobfoo%40example.com:p%40ssw0r&:d@github.com/gocd/gocd")
                , Arguments.of("git@example.com/ddd", "git@example.com/ddd", null, null, "git@example.com/ddd")
                , Arguments.of("git@example.com:8154/ddd", "git@example.com:8154/ddd", null, null, "git@example.com:8154/ddd")
                , Arguments.of("https://", "https://", null, null, "https://")
                , Arguments.of("http://", "http://", null, null, "http://")
        );
    }
}
