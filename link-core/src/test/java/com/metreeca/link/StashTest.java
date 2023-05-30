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

import java.util.List;

import static com.metreeca.link.Stash.Transform.*;
import static com.metreeca.link.Stash.alias;
import static com.metreeca.link.Stash.expression;

import static org.assertj.core.api.Assertions.*;

final class StashTest {

    @Nested final class Expressions {

        @Test void testDecodeAliases() {

            assertThat(alias("field"))
                    .isEmpty();

            assertThat(alias("alias="))
                    .contains(entry("alias", expression("")));

            assertThat(alias("alias=field"))
                    .contains(entry("alias", expression("field")));

            assertThat(alias("'alias'=field"))
                    .contains(entry("alias", expression("field")));

            assertThat(alias("'x=y'=field"))
                    .contains(entry("x=y", expression("field")));

        }


        @Test void testDecodeIdentifierFields() {
            assertThat(expression("id"))
                    .isEqualTo(expression(List.of("id"), List.of()));
        }

        @Test void testDecodeQuotedFields() {
            assertThat(expression("'!\\'\\\\'"))
                    .isEqualTo(expression(List.of("!'\\"), List.of()));
        }

        @Test void testReportMalformedFieldNames() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> expression("0è"));
        }


        @Test void testDecodeEmptyPaths() {
            assertThat(expression(""))
                    .isEqualTo(expression(List.of(), List.of()));
        }

        @Test void testDecodePaths() {
            assertThat(expression("x.y.z"))
                    .isEqualTo(expression(List.of("x", "y", "z"), List.of()));
        }

        @Test void testDecodeQuotedPaths() {
            assertThat(expression("x.'y+w'.z"))
                    .isEqualTo(expression(List.of("x", "y+w", "z"), List.of()));
        }


        @Test void testDecodeTransforms() {
            assertThat(expression("count:field"))
                    .isEqualTo(expression(List.of("field"), List.of(count)));
        }

        @Test void testDecodeTransformPipes() {
            assertThat(expression("sum:abs:field"))
                    .isEqualTo(expression(List.of("field"), List.of(sum, abs)));
        }

        @Test void testDecodeTransformPipesOnEmptyPaths() {
            assertThat(expression("sum:abs:"))
                    .isEqualTo(expression(List.of(), List.of(sum, abs)));
        }

        @Test void testReportUnknownTransforms() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> expression("none:field"));
        }

    }

}