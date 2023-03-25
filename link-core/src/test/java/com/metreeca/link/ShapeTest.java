/*
 * Copyright © 2023-2024 Metreeca srl
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

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map.Entry;
import java.util.function.Supplier;

import static com.metreeca.link.Shape.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class ShapeTest {

    private static final Shape recursive=property(RDF.VALUE, new Supplier<>() {

        @Override public Shape get() { return recursive; }

    });


    @Test void testHandleRecursiveDefinitions() {
        assertThat(recursive.entry(RDF.VALUE).map(Entry::getValue))
                .contains(recursive);
    }


    @Nested
    final class Base {

        @Test void testReportRelativeBases() {
            assertThatIllegalArgumentException().isThrownBy(() -> base("/path"));
        }


        @Test void testResolve() {

            final Shape base=base("https://example.org/path/");

            assertThat(base.resolve("name")).isEqualTo("https://example.org/path/name");
            assertThat(base.resolve("https://example.net/")).isEqualTo("https://example.net/");
        }

        @Test void testResolveWithUndefinedBase() {
            assertThat(shape().resolve("https://example.org/")).isEqualTo("https://example.org/");
            assertThatIllegalArgumentException().isThrownBy(() -> shape().resolve("/path"));
        }


        @Test void testRelativize() {

            final Shape base=base("https://example.org/path/");

            assertThat(base.relativize("https://example.org/name")).isEqualTo("/name");
            assertThat(base.relativize("https://example.net/")).isEqualTo("https://example.net/");
        }

        @Test void testRelativizeWithUndefinedBase() {
            assertThat(shape().relativize("/path")).isEqualTo("/path");
        }

    }

}