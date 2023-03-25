/*
 * Copyright © 2023-2024 Metreeca srl
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
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.metreeca.link.json.JSONReader.Type.STRING;
import static com.metreeca.link.json.JSONReader.Type.*;

import static java.lang.String.format;
import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.*;

final class JSONReaderTest {

    private static JSONReader reader(final String json) {
        return new JSONReader(new StringReader(json));
    }


    private Map.Entry<JSONReader.Type, String> next(final JSONReader reader) throws IOException {
        return entry(reader.type(), reader.token());
    }


    @Nested final class Objects {

        @Test void testReadEmptyObject() throws IOException {

            final JSONReader reader=reader("{ }");

            assertThat(next(reader)).isEqualTo(entry(LBRACE, "{"));
            assertThat(next(reader)).isEqualTo(entry(RBRACE, "}"));

        }

        @Test void testReadSingletonObject() throws IOException {

            final JSONReader reader=reader("{ \"label\" : \"value\" }");

            assertThat(next(reader)).isEqualTo(entry(LBRACE, "{"));
            assertThat(next(reader)).isEqualTo(entry(STRING, "label"));
            assertThat(next(reader)).isEqualTo(entry(COLON, ":"));
            assertThat(next(reader)).isEqualTo(entry(STRING, "value"));
            assertThat(next(reader)).isEqualTo(entry(RBRACE, "}"));

        }

        @Test void testReadExtendedObject() throws IOException {

            final JSONReader reader=reader("{ \"one\" : 1, \"two\" : 2 }");

            assertThat(next(reader)).isEqualTo(entry(LBRACE, "{"));
            assertThat(next(reader)).isEqualTo(entry(STRING, "one"));
            assertThat(next(reader)).isEqualTo(entry(COLON, ":"));
            assertThat(next(reader)).isEqualTo(entry(NUMBER, "1"));
            assertThat(next(reader)).isEqualTo(entry(COMMA, ","));
            assertThat(next(reader)).isEqualTo(entry(STRING, "two"));
            assertThat(next(reader)).isEqualTo(entry(COLON, ":"));
            assertThat(next(reader)).isEqualTo(entry(NUMBER, "2"));
            assertThat(next(reader)).isEqualTo(entry(RBRACE, "}"));

        }

    }

    @Nested final class Arrays {

        @Test void testReadEmptyArray() throws IOException {

            final JSONReader reader=reader("[ ]");

            assertThat(next(reader)).isEqualTo(entry(LBRACKET, "["));
            assertThat(next(reader)).isEqualTo(entry(RBRACKET, "]"));

        }

        @Test void testReadSingletonArray() throws IOException {

            final JSONReader reader=reader("[ \"value\" ]");

            assertThat(next(reader)).isEqualTo(entry(LBRACKET, "["));
            assertThat(next(reader)).isEqualTo(entry(STRING, "value"));
            assertThat(next(reader)).isEqualTo(entry(RBRACKET, "]"));

        }

        @Test void testReadExtendedArray() throws IOException {

            final JSONReader reader=reader("[ 1,  2 ]");

            assertThat(next(reader)).isEqualTo(entry(LBRACKET, "["));
            assertThat(next(reader)).isEqualTo(entry(NUMBER, "1"));
            assertThat(next(reader)).isEqualTo(entry(COMMA, ","));
            assertThat(next(reader)).isEqualTo(entry(NUMBER, "2"));
            assertThat(next(reader)).isEqualTo(entry(RBRACKET, "]"));

        }

    }

    @Nested final class Strings {

        @Test void testReadString() throws IOException {

            final JSONReader reader=reader("  \"a string\",");

            assertThat(next(reader)).isEqualTo(entry(STRING, "a string"));

        }

        @Test void testReadEscapedString() throws IOException {

            final JSONReader reader=reader("\"\\\"\\\\\\/\\b\\f\\n\\r\\t\\u0003\"");

            assertThat(next(reader)).isEqualTo(entry(STRING, "\"\\/\b\f\n\r\t\u0003"));

        }

        @Test void testReportIncompleteEscapeSequence() {

            final JSONReader reader=reader("\"\\\"");

            assertThatExceptionOfType(JSONException.class).isThrownBy(() -> next(reader));
        }

        @Test void testReportMalformedEscapeSequence() {

            final JSONReader reader=reader("\"\\x\"");

            assertThatExceptionOfType(JSONException.class).isThrownBy(() -> next(reader));
        }

        @Test void testReportIncompleteUnicodeEscapeSequence() {

            final JSONReader reader=reader("\"\\u00\"");

            assertThatExceptionOfType(JSONException.class).isThrownBy(() -> next(reader));
        }

        @Test void testReportMalformedUnicodeEscapeSequence() {

            final JSONReader reader=reader("\"\\u00Z0\"");

            assertThatExceptionOfType(JSONException.class).isThrownBy(() -> next(reader));
        }

    }

    @Nested final class Numbers {

        @Test void testReadNumbers() {
            List.of(

                    "0",
                    "-0",
                    "123",
                    "-123",
                    "123.456",
                    "-123.456",
                    "123.456e123",
                    "-123.456E123",
                    "123.456e-123",
                    "-123.456E-123",
                    "123.456e+123",
                    "-123.456E+123"

            ).forEach(number -> {

                try {

                    final JSONReader parser=reader(format(" %s, ", number));

                    assertThat(next(parser)).isEqualTo(entry(NUMBER, number.toUpperCase(Locale.ROOT)));

                } catch ( final IOException e ) {
                    throw new UncheckedIOException(e);
                }

            });
        }

        @Test void testReportMalformedNumbers() {
            List.of(

                    "0123",
                    "123."

            ).forEach(number -> {

                assertThatRuntimeException()
                        .isThrownBy(() -> reader(format(" %s, ", number)).type());

            });
        }

    }

    @Nested final class Literals {

        @Test void testReadTrue() throws IOException {

            final JSONReader parser=reader("  true,");

            assertThat(next(parser)).isEqualTo(entry(TRUE, "true"));

        }

        @Test void testReadFalse() throws IOException {

            final JSONReader parser=reader("  false,");

            assertThat(next(parser)).isEqualTo(entry(FALSE, "false"));

        }

        @Test void testReadNull() throws IOException {

            final JSONReader parser=reader("  null,");

            assertThat(next(parser)).isEqualTo(entry(NULL, "null"));

        }


        @Test void testReportTrailingTest() throws IOException {
            assertThatRuntimeException()
                    .isThrownBy(() -> reader("  nullnull,").type());
        }

    }


    @Test void testSkipWhitespace() throws IOException {
        assertThat(next(reader("  \t\r\n"))).isEqualTo(entry(EOF, ""));
    }

    @Test void testHandleEOF() throws IOException {
        assertThat(next(reader("true"))).isEqualTo(entry(TRUE, "true"));
    }

    @Test void testReportEOF() throws IOException {
        assertThat(next(reader(""))).isEqualTo(entry(EOF, ""));
    }

}