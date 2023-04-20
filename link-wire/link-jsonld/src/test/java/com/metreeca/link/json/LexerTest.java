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

import java.io.*;
import java.util.*;

import static com.metreeca.link.json.JSON.Tokens.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

import static java.lang.String.format;
import static java.util.Map.entry;

final class LexerTest {

    private static com.metreeca.link.json.Lexer lexer(final String json) {
        return new com.metreeca.link.json.Lexer(new StringReader(json));
    }


    private Map.Entry<com.metreeca.link.json.JSON.Tokens, String> next(final com.metreeca.link.json.Lexer parser) throws IOException {
        return entry(parser.type(), parser.token());
    }


    @Nested final class Objects {

        @Test void testParseEmptyObject() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("{ }");

            assertThat(next(parser)).isEqualTo(entry(LBRACE, "{"));
            assertThat(next(parser)).isEqualTo(entry(RBRACE, "}"));

        }

        @Test void testParseSingletonObject() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("{ \"label\" : \"value\" }");

            assertThat(next(parser)).isEqualTo(entry(LBRACE, "{"));
            assertThat(next(parser)).isEqualTo(entry(STRING, "label"));
            assertThat(next(parser)).isEqualTo(entry(COLON, ":"));
            assertThat(next(parser)).isEqualTo(entry(STRING, "value"));
            assertThat(next(parser)).isEqualTo(entry(RBRACE, "}"));

        }

        @Test void testParseExtendedObject() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("{ \"one\" : 1, \"two\" : 2 }");

            assertThat(next(parser)).isEqualTo(entry(LBRACE, "{"));
            assertThat(next(parser)).isEqualTo(entry(STRING, "one"));
            assertThat(next(parser)).isEqualTo(entry(COLON, ":"));
            assertThat(next(parser)).isEqualTo(entry(NUMBER, "1"));
            assertThat(next(parser)).isEqualTo(entry(COMMA, ","));
            assertThat(next(parser)).isEqualTo(entry(STRING, "two"));
            assertThat(next(parser)).isEqualTo(entry(COLON, ":"));
            assertThat(next(parser)).isEqualTo(entry(NUMBER, "2"));
            assertThat(next(parser)).isEqualTo(entry(RBRACE, "}"));

        }

    }

    @Nested final class Arrays {

        @Test void testParseEmptyArray() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("[ ]");

            assertThat(next(parser)).isEqualTo(entry(LBRACKET, "["));
            assertThat(next(parser)).isEqualTo(entry(RBRACKET, "]"));

        }

        @Test void testParseSingletonArray() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("[ \"value\" ]");

            assertThat(next(parser)).isEqualTo(entry(LBRACKET, "["));
            assertThat(next(parser)).isEqualTo(entry(STRING, "value"));
            assertThat(next(parser)).isEqualTo(entry(RBRACKET, "]"));

        }

        @Test void testParseExtendedArrayt() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("[ 1,  2 ]");

            assertThat(next(parser)).isEqualTo(entry(LBRACKET, "["));
            assertThat(next(parser)).isEqualTo(entry(NUMBER, "1"));
            assertThat(next(parser)).isEqualTo(entry(COMMA, ","));
            assertThat(next(parser)).isEqualTo(entry(NUMBER, "2"));
            assertThat(next(parser)).isEqualTo(entry(RBRACKET, "]"));

        }

    }

    @Nested final class Strings {

        @Test void testParseString() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("  \"string\",");

            assertThat(next(parser)).isEqualTo(entry(STRING, "string"));

        }

        // !!! escapes
        // !!! malformed escapes
        // !!! control chars

    }

    @Nested final class Numbers {

        @Test void testParseNumbers() {
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

                    final com.metreeca.link.json.Lexer parser=lexer(format(" %s, ", number));

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
                        .isThrownBy(() -> lexer(format(" %s, ", number)).type());

            });
        }

    }

    @Nested final class Literals {

        @Test void testParseTrue() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("  true,");

            assertThat(next(parser)).isEqualTo(entry(TRUE, "true"));

        }

        @Test void testParseFalse() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("  false,");

            assertThat(next(parser)).isEqualTo(entry(FALSE, "false"));

        }

        @Test void testParseNull() throws IOException {

            final com.metreeca.link.json.Lexer parser=lexer("  null,");

            assertThat(next(parser)).isEqualTo(entry(NULL, "null"));

        }


        @Test void testReportTrailingTest() throws IOException {
            assertThatRuntimeException()
                    .isThrownBy(() -> lexer("  nullnull,").type());
        }

    }


    @Test void testSkipWhitespace() throws IOException {
        assertThat(next(lexer("  \t\r\n"))).isEqualTo(entry(EOF, ""));
    }

    @Test void testHandleEOF() throws IOException {
        assertThat(next(lexer("true"))).isEqualTo(entry(TRUE, "true"));
    }

    @Test void testReportEOF() throws IOException {
        assertThat(next(lexer(""))).isEqualTo(entry(EOF, ""));
    }

}