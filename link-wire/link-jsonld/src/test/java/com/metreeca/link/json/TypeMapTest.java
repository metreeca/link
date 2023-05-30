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

import java.util.LinkedHashMap;
import java.util.Map;

import static com.metreeca.link.Stash.integer;
import static com.metreeca.link.json.JSONTest.*;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

final class TypeMapTest {

    @Nested final class Encode {

        @Test void testEncodeEmptyMap() {
            assertThat(encode(Map.of())).isEqualTo("{}");
        }

        @Test void testEncodeSingletonMap() {

            final Map<String, Object> map=Map.of("label", "value");

            assertThat(encode(map)).isEqualTo("{\"label\":\"value\"}");
        }

        @Test void testEncodeExtendedMap() {

            final Map<String, Object> map=new LinkedHashMap<>();

            map.put("one", 1);
            map.put("two", 2);

            assertThat(encode(map)).isEqualTo("{\"one\":1,\"two\":2}");
        }

        @Test void testEncodePrettyMap() {

            final Map<String, Object> map=new LinkedHashMap<>();

            map.put("one", Map.of());
            map.put("two", Map.of("label", "value"));

            assertThat(pretty(map)).isEqualTo("{\n"
                    +"  \"one\": {},\n"
                    +"  \"two\": {\n"
                    +"    \"label\": \"value\"\n"
                    +"  }\n"
                    +"}");
        }

    }

    @Nested final class Decode {

        @Test void testDecodeEmptyMap() {
            assertThat(decode("{}", Map.class)).isEqualTo(Map.of());
        }

        @Test void testDecodeSingletonMap() {
            assertThat(decode("{\"one\":1}", Map.class))
                    .isEqualTo(Map.ofEntries(entry("one", integer(1))));
        }

        @Test void testDecodeExtendedMap() {
            assertThat(decode("{\"one\":1,\"two\":2,\"three\":3}", Map.class))
                    .isEqualTo(Map.ofEntries(
                            entry("one", integer(1)),
                            entry("two", integer(2)),
                            entry("three", integer(3))
                    ));
        }

    }

}