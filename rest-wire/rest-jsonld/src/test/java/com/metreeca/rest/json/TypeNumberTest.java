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

package com.metreeca.rest.json;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.rest.json.JSONTest.*;

import static org.assertj.core.api.Assertions.assertThat;

final class TypeNumberTest {

    @Nested final class Encode {

        @Test void testEncodeIntegers() {
            assertThat(encode(123)).isEqualTo("123");
            assertThat(encode(-123)).isEqualTo("-123");
        }

        @Test void testEncodeDecimals() {
            assertThat(encode(123.345)).isEqualTo("123.345");
            assertThat(encode(-123.345)).isEqualTo("-123.345");
        }

        @Test void testEncodeReals() {
            assertThat(encode(1.345e56)).isEqualTo("1.345E56");
            assertThat(encode(-1.345e56)).isEqualTo("-1.345E56");
            assertThat(encode(1.345e-56)).isEqualTo("1.345E-56");
            assertThat(encode(-1.345e-56)).isEqualTo("-1.345E-56");
        }

    }

    @Nested final class Decode {

        @Test void testDecodeIntegers() {
            assertThat(decode("123", Number.class)).isEqualTo(integer(123));
            assertThat(decode("-123", Number.class)).isEqualTo(integer(-123));
        }

        @Test void testDecodeDecimals() {
            assertThat(decode("123.345", Number.class)).isEqualTo(decimal(123.345));
            assertThat(decode("-123.345", Number.class)).isEqualTo(decimal(-123.345));
        }

        @Test void testDecodeReals() {
            assertThat(decode("1.345e56", Number.class)).isEqualTo(1.345e56);
            assertThat(decode("-1.345e56", Number.class)).isEqualTo(-1.345e56);
            assertThat(decode("1.345e-56", Number.class)).isEqualTo(1.345e-56);
            assertThat(decode("-1.345e-56", Number.class)).isEqualTo(-1.345e-56);
        }

    }

}