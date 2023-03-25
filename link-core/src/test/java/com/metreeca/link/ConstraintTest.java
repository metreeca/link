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

import org.eclipse.rdf4j.model.Literal;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.metreeca.link.Constraint.*;
import static com.metreeca.link.Frame.literal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class ConstraintTest {

    @Test void testMergeLt() {
        assertThat(and(lt(literal(10)), lt(literal(100))).lt())
                .contains(literal(10));
    }

    @Test void testMergeGt() {
        assertThat(and(gt(literal(10)), gt(literal(100))).gt())
                .contains(literal(100));
    }

    @Test void testMergeLte() {
        assertThat(and(lte(literal(10)), lte(literal(100))).lte())
                .contains(literal(10));
    }

    @Test void testMergeGte() {
        assertThat(and(gte(literal(10)), gte(literal(100))).gte())
                .contains(literal(100));
    }


    @Test void testMergeLike() {

        assertThat(and(like("x"), like("x")).like())
                .contains("x");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> and(like("x"), like("y")));

    }

    @Test void testMergeAny() {

        final Literal x=literal("x");
        final Literal y=literal("y");
        final Literal z=literal("z");

        assertThat(and(any(x), any(x)).any())
                .contains(Set.of(x));

        assertThat(and(any(x, y, z), any(x, y)).any())
                .contains(Set.of(x, y));

        assertThat(and(any(x, y), any(x, y, z)).any())
                .contains(Set.of(x, y));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> and(any(x, y), any(x, z)));

    }

}
