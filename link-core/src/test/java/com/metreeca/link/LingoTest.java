/*
 * Copyright © 2023 Metreeca srl
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

package com.metreeca.link;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.Lingo.*;

import static org.assertj.core.api.Assertions.assertThat;

final class LingoTest {

    @Nested
    final class Root {

        @Test void testRoot() {
            assertThat(root("https://example.com/")).contains("https://example.com/");
        }

        @Test void testPath() {
            assertThat(root("https://example.com/path/")).contains("https://example.com/");
        }

    }

    @Nested
    final class Path {

        @Test void testRoot() {
            assertThat(path("https://example.com/")).contains("/");
        }

        @Test void testPath() {
            assertThat(path("https://example.com/path/")).contains("/path/");
        }

    }

    @Nested
    final class Name {

        @Test void testRoot() {
            assertThat(name("https://example.com/")).isEmpty();
        }

        @Test void testPath() {
            assertThat(name("https://example.com/path/")).isEmpty();
        }

        @Test void testSlashName() {
            assertThat(name("https://example.com/path/name")).contains("name");
        }

        @Test void testHashName() {
            assertThat(name("https://example.com/path#name")).contains("name");
        }

    }

}