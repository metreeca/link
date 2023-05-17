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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.metreeca.link.Query.integer;
import static com.metreeca.link.json.JSONTest.*;

import static org.assertj.core.api.Assertions.assertThat;

final class TypeCollectionTest {

    @Nested final class Encode {

        @Test void testEncodeEmptyCollection() {
            assertThat(encode(List.of())).isEqualTo("[]");
        }

        @Test void testEncodeSingletonCollection() {
            assertThat(encode(List.of(1))).isEqualTo("[1]");
        }

        @Test void testEncodeExtendedCollection() {
            assertThat(encode(List.of(1, 2, 3))).isEqualTo("[1,2,3]");
        }

        @Test void testEncodePrettyCollection() {

            final List<Object> collection=List.of(
                    List.of(),
                    List.of("value")
            );

            assertThat(pretty(collection)).isEqualTo("[\n"
                    +"  [],\n"
                    +"  [\n"
                    +"    \"value\"\n"
                    +"  ]\n"
                    +"]");
        }

    }

    @Nested final class Decode {

        @Test void testDecodeEmptyCollection() {
            assertThat(decode("[]", Collection.class))
                    .isEmpty();
        }

        @Test void testDecodeSingletonCollection() {
            assertThat(decode("[1]", Collection.class))
                    .containsExactly(integer(1));
        }

        @Test void testDecodeExtendedCollection() {
            assertThat(decode("[1,2,3]", Collection.class))
                    .containsExactly(integer(1), integer(2), integer(3));
        }


        @Test void testDecodeSets() {
            assertThat(decode("[1,1,2,2,3,3]", Set.class))
                    .isInstanceOf(Set.class)
                    .containsExactly(integer(1), integer(2), integer(3));
        }

        @Test void testDecodeLists() {
            assertThat(decode("[1,2,3]", List.class))
                    .isInstanceOf(List.class)
                    .containsExactly(integer(1), integer(2), integer(3));
        }

    }

}