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

import java.util.Set;

import static com.metreeca.link.Query.*;

import static org.assertj.core.api.Assertions.assertThat;

final class QueryTest {

    @Nested final class Constraints {

        @Test void testMergeLt() {
            assertThat(and(lt(10), lt(100)))
                    .isEqualTo(lt(10));
        }

        @Test void testMergeGt() {
            assertThat(and(gt(10), gt(100)))
                    .isEqualTo(gt(100));
        }

        @Test void testMergeLte() {
            assertThat(and(lte(10), lte(100)))
                    .isEqualTo(lte(10));
        }

        @Test void testMergeGte() {
            assertThat(and(gte(10), gte(100)))
                    .isEqualTo(gte(100));
        }


        @Test void testMergeLike() {

            assertThat(and(like("x"), like("x")).like())
                    .containsExactly("x");

            assertThat(and(like("x"), like("y")).like())
                    .containsExactly("x", "y");

        }

        @Test void testMergeAny() {

            assertThat(and(any("x"), any("x")).any())
                    .containsExactly(Set.of("x"));

            assertThat(and(any("x"), any("y")).any())
                    .containsExactly(Set.of("x"), Set.of("y"));

        }

    }

}