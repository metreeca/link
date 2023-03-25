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

import java.io.IOException;
import java.net.URI;

import static com.metreeca.rest.json.JSONTest.decode;
import static com.metreeca.rest.json.JSONTest.encode;

import static org.assertj.core.api.Assertions.assertThat;

final class TypeURITest {

    @Nested final class Encode {

        @Test void testEncodeURIs() throws IOException {
            assertThat(encode(URI.create("https://ex.com/absolute"))).isEqualTo("https://ex.com/absolute");
            assertThat(encode(URI.create("/relative"))).isEqualTo("/relative");
        }

    }

    @Nested final class Decode {

        @Test void testDecodeURIs() throws IOException {
            assertThat(decode("\"https://ex.com/absolute\"", URI.class)).isEqualTo(URI.create("https://ex"
                    +".com/absolute"));
            assertThat(decode("\"/relative\"", URI.class)).isEqualTo(URI.create("/relative"));
        }

    }

}