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

package com.metreeca.link.specs;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.metreeca.link.specs.Constraint.and;

import static org.assertj.core.api.Assertions.assertThat;

final class ConstraintTest {

    @Test void testMergeLt() {
        assertThat(and(Constraint.lt(10), Constraint.lt(100)))
                .isEqualTo(Constraint.lt(10));
    }

    @Test void testMergeGt() {
        assertThat(and(Constraint.gt(10), Constraint.gt(100)))
                .isEqualTo(Constraint.gt(100));
    }

    @Test void testMergeLte() {
        assertThat(and(Constraint.lte(10), Constraint.lte(100)))
                .isEqualTo(Constraint.lte(10));
    }

    @Test void testMergeGte() {
        assertThat(and(Constraint.gte(10), Constraint.gte(100)))
                .isEqualTo(Constraint.gte(100));
    }


    @Test void testMergeLike() {

        assertThat(and(Constraint.like("x"), Constraint.like("x")).like())
                .containsExactly("x");

        assertThat(and(Constraint.like("x"), Constraint.like("y")).like())
                .containsExactly("x", "y");

    }

    @Test void testMergeAny() {

        assertThat(and(Constraint.any("x"), Constraint.any("x")).any())
                .containsExactly(Set.of("x"));

        assertThat(and(Constraint.any("x"), Constraint.any("y")).any())
                .containsExactly(Set.of("x"), Set.of("y"));

    }

}
