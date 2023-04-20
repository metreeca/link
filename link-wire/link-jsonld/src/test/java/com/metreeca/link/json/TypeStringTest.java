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

import java.io.IOException;

import static com.metreeca.link.json.JSONTest.encode;

import static org.assertj.core.api.Assertions.assertThat;

final class TypeStringTest {

    @Nested final class Encode {

        @Test void testEncodeEmptyString() throws IOException {
            assertThat(encode("")).isEqualTo("\"\"");
        }

        @Test void testEncodePlainString() throws IOException {
            assertThat(encode("ciao!")).isEqualTo("\"ciao!\"");
        }

        @Test void testEscapesSpecialCharacter() throws IOException {
            assertThat(encode("\u0003\b\f\n\r\t \"\\"))
                    .isEqualTo("\"\\u0003\\b\\f\\n\\r\\t \\\"\\\\\"");
        }

    }

    @Nested final class Decode {

        // !!! unescape \/

    }

}