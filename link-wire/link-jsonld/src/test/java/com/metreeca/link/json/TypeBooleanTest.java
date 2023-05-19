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

package com.metreeca.link.json;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.link.json.JSONTest.decode;
import static com.metreeca.link.json.JSONTest.encode;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.assertj.core.api.Assertions.assertThat;

final class TypeBooleanTest {

    @Nested final class Encode {

        @Test void testEncodePrimitive() {
            assertThat(encode(true)).isEqualTo("true");
            assertThat(encode(false)).isEqualTo("false");
        }

        @Test void testEncodeBoxed() {
            assertThat(encode(TRUE)).isEqualTo("true");
            assertThat(encode(FALSE)).isEqualTo("false");
        }

    }

    @Nested
    final class Decode {

        @Test void testDecodePrimitive() {
            assertThat(decode("true", boolean.class)).isEqualTo(true);
            assertThat(decode("false", boolean.class)).isEqualTo(false);
        }

        @Test void testDecodeBoxed() {
            assertThat(decode("true", Boolean.class)).isEqualTo(true);
            assertThat(decode("false", Boolean.class)).isEqualTo(false);
        }

    }

}