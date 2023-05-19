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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static com.metreeca.link.json.JSONTest.decode;
import static com.metreeca.link.json.JSONTest.encode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class TypeLocalDateTimeTest {

    private static final String encoded="\"2023-05-19T16:05:12\"";

    private static final LocalDateTime decoded=LocalDateTime.of(
            LocalDate.of(2023, 5, 19),
            LocalTime.of(16, 5, 12)
    );


    @Nested
    final class Encode {

        @Test void testEncode() {
            assertThat(encode(decoded))
                    .isEqualTo(encoded);
        }

    }

    @Nested
    final class Decode {

        @Test void testDecode() {
            assertThat(decode(encoded, LocalDateTime.class))
                    .isEqualTo(decoded);
        }

        @Test void testReport() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("malformed", LocalDateTime.class));
        }

    }

}