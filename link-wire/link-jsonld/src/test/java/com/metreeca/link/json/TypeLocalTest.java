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

import com.metreeca.link.Local;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static com.metreeca.link.Local.local;
import static com.metreeca.link.json.JSONTest.decode;
import static com.metreeca.link.json.JSONTest.encode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class TypeLocalTest {

    @Nested
    final class Encode {

        @Test void testEncodeEmptyLocal() {
            assertThat(encode(local()))
                    .isEqualTo("{}");
        }

        @Test void testEncodeUniqueLocal() {
            assertThat(encode(local(local("en", "one"), local("it", "uno"))))
                    .isEqualTo("{\"en\":\"one\",\"it\":\"uno\"}");
        }

        @Test void testEncodeCommonLocal() {
            assertThat(encode(local(local("en", List.of("one", "two")), local("it", List.of("uno", "due")))))
                    .isEqualTo("{\"en\":[\"one\",\"two\"],\"it\":[\"uno\",\"due\"]}");
        }

        @Test void testEncodeRootLocal() {
            assertThat(encode(local(Locale.ROOT, "value")))
                    .isEqualTo("{\"\":\"value\"}");
        }

        @Test void testEncodeWildcardLocal() {
            assertThat(encode(local(Local.Wildcard, "value")))
                    .isEqualTo("{\"\":\"value\"}");
        }

    }

    @Nested
    final class Decode {

        @Test void testDecodeEmptyLocal() {
            assertThat(decode("{}", Local.class))
                    .isEqualTo(local());
        }

        @Test void testDecodeUniqueLocal() {
            assertThat(decode("{\"en\":\"one\",\"it\":\"uno\"}", Local.class))
                    .isEqualTo(local(local("en", "one"), local("it", "uno")));
        }

        @Test void testDecodeCommonLocal() {
            assertThat(decode("{\"en\":[\"one\",\"two\"],\"it\":[\"uno\",\"due\"]}", Local.class))
                    .isEqualTo(local(local("en", List.of("one", "two")), local("it", List.of("uno", "due"))));
        }

        @Test void testDecodeRootLocal() {
            assertThat(decode("{\"\":\"value\"}", Local.class))
                    .isEqualTo(local(Locale.ROOT, "value"));
        }

        @Test void testDecodeWildcardLocal() {
            assertThat(decode("{\"*\":\"value\"}", Local.class))
                    .isEqualTo(local(Local.Wildcard, "value"));
        }

        @Test void testReportMixedLocal() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("{\"en\":[\"one\",\"two\"],\"it\":\"uno\"}", Local.class));
        }

        @Test void testReportMalformedLocal() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("{\"en\":0}", Local.class));
        }

    }

}