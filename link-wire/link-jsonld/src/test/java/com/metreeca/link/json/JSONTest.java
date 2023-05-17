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

import org.junit.jupiter.api.Test;

import static com.metreeca.link.json.JSON.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class JSONTest {

    public static class Bean { }


    static String pretty(final Object value) {
        return json().pretty(true).encode(value);
    }

    static String encode(final Object value) {
        return json().encode(value);
    }

    static <T> T decode(final String json, final Class<T> clazz) {
        return json().decode(json, clazz);
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test void testDecodeJSON() {
        assertThat(decode("{}", Bean.class)).isInstanceOf(Bean.class);
    }

    @Test void testDecodeURLEncodedJSON() {
        assertThat(decode("%7B%7D", Bean.class)).isInstanceOf(Bean.class);
    }

    @Test void testDecodeBase64JSON() {
        assertThat(decode("e30=", Bean.class)).isInstanceOf(Bean.class);
    }


    @Test void testReportLocation() {

        assertThatExceptionOfType(JSONException.class)
                .isThrownBy(() -> decode("nullnull", Object.class))
                .withMessageStartingWith("(1,5)")
                .satisfies(e -> assertThat(e.getLine()).isEqualTo(1))
                .satisfies(e -> assertThat(e.getCol()).isEqualTo(5));

    }


    @Test void testReportUnexpectedEOF() {
        assertThatExceptionOfType(JSONException.class)
                .isThrownBy(() -> decode("{ ", Object.class))
                .withMessageStartingWith("(1,3)")
                .satisfies(e -> assertThat(e.getLine()).isEqualTo(1))
                .satisfies(e -> assertThat(e.getCol()).isEqualTo(3));

    }

    @Test void testReportTrailingGarbage() {
        assertThatExceptionOfType(JSONException.class)
                .isThrownBy(() -> decode("{} {}", Object.class))
                .withMessageStartingWith("(1,4)")
                .satisfies(e -> assertThat(e.getLine()).isEqualTo(1))
                .satisfies(e -> assertThat(e.getCol()).isEqualTo(4));
    }


    @Test void testReportUnexpectedQuery() {
        assertThatExceptionOfType(JSONException.class)
                .isThrownBy(() -> { final Bean decode=decode("{ \"#\":  0 }", Bean.class); });
    }

}