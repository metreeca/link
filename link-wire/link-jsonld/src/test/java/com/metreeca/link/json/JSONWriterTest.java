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

import java.io.StringWriter;
import java.util.function.Consumer;

import static com.metreeca.link.json.JSON.json;

import static org.assertj.core.api.Assertions.assertThat;

final class JSONWriterTest {

    private static String write(final Consumer<JSONWriter> task) { return write(false, task); }

    private static String write(final boolean pretty, final Consumer<JSONWriter> task) {

        final StringWriter writer=new StringWriter();

        task.accept(new JSONWriter(json().pretty(pretty), writer));

        return writer.toString();
    }

    private static String write(final String string) {
        return string
                .replace('\'', '"');
    }


    @Nested
    final class Objects {

        @Test void testWriteEmptyObject() {
            assertThat(write(writer -> {

                writer.object(true);
                writer.object(false);

            })).isEqualTo(write(

                    "{}"

            ));
        }

        @Test void testWriteSingletonObject() {
            assertThat(write(writer -> {

                writer.object(true);
                writer.comma();
                writer.string("label");
                writer.colon();
                writer.string("value");
                writer.object(false);

            })).isEqualTo(write(

                    "{'label':'value'}"

            ));
        }

        @Test void testWriteFullObject() {
            assertThat(write(writer -> {

                writer.object(true);

                writer.comma();
                writer.string("one");
                writer.colon();
                writer.string("uno");

                writer.comma();
                writer.string("two");
                writer.colon();
                writer.string("due");

                writer.object(false);

            })).isEqualTo(write(

                    "{'one':'uno','two':'due'}"

            ));
        }

    }

    @Nested
    final class Arrays {

        @Test void testWriteEmptyArray() {
            assertThat(write(writer -> {

                writer.array(true);
                writer.array(false);

            })).isEqualTo(write(

                    "[]"

            ));
        }

        @Test void testWriteSingletonArray() {
            assertThat(write(writer -> {

                writer.array(true);
                writer.comma();
                writer.string("value");
                writer.array(false);

            })).isEqualTo(write(

                    "['value']"

            ));
        }

        @Test void testWriteFullArray() {
            assertThat(write(writer -> {

                writer.array(true);

                writer.comma();
                writer.string("one");

                writer.comma();
                writer.string("two");

                writer.array(false);

            })).isEqualTo(write(

                    "['one','two']"

            ));
        }

    }

    @Nested
    final class Literals {

        @Test void testWriteLiteral() {
            assertThat(write(writer -> writer.literal("literal")))
                    .isEqualTo(write("literal"));
        }

        @Test void testWriteEmptyString() {
            assertThat(write(writer -> writer.string("")))
                    .isEqualTo(write("\"\""));
        }

        @Test void testWritePlainString() {
            assertThat(write(writer -> writer.string("ciao!")))
                    .isEqualTo(write("\"ciao!\""));
        }

        @Test void testWriteEscapedString() {
            assertThat(write(writer -> writer.string("\"\\\b\f\n\r\t\u0003")))
                    .isEqualTo(write("\"\\\"\\\\\\b\\f\\n\\r\\t\\u0003\""));
        }

    }

    @Nested
    final class Pretty {

        @Test void testWriteEmptyObject() {
            assertThat(write(true, writer -> {

                writer.object(true);
                writer.object(false);

            })).isEqualTo(write(

                    "{}"

            ));
        }

        @Test void testWriteSingletonObject() {
            assertThat(write(true, writer -> {

                writer.object(true);
                writer.comma();
                writer.string("label");
                writer.colon();
                writer.string("value");
                writer.object(false);

            })).isEqualTo(write(

                    "{\n"+
                            "    'label': 'value'\n"+
                            "}"

            ));
        }

        @Test void testWriteFullObject() {
            assertThat(write(true, writer -> {

                writer.object(true);

                writer.comma();
                writer.string("one");
                writer.colon();
                writer.string("uno");

                writer.comma();
                writer.string("two");
                writer.colon();
                writer.string("due");

                writer.object(false);

            })).isEqualTo(write(

                    "{\n"+
                            "    'one': 'uno',\n"+
                            "    'two': 'due'\n"+
                            "}"

            ));
        }


        @Test void testWriteEmptyArray() {
            assertThat(write(true, writer -> {

                writer.array(true);
                writer.array(false);

            })).isEqualTo(write(

                    "[]"

            ));
        }


        @Test void testWriteSingletonArray() {
            assertThat(write(true, writer -> {

                writer.array(true);
                writer.comma();
                writer.string("value");
                writer.array(false);

            })).isEqualTo(write(

                    "[\n"+
                            "    'value'\n"+
                            "]"

            ));
        }

        @Test void testWriteFullArray() {
            assertThat(write(true, writer -> {

                writer.array(true);

                writer.comma();
                writer.string("one");

                writer.comma();
                writer.string("two");

                writer.array(false);

            })).isEqualTo(write(

                    "[\n"+
                            "    'one',\n"+
                            "    'two'\n"+
                            "]"

            ));
        }


        @Test void testWriteNestedStructures() {
            // language=TEXT
            assertThat(write(true, writer -> {

                writer.object(true);

                writer.string("");
                writer.colon();
                writer.string("");

                writer.comma();

                writer.array(true);

                writer.string("");

                writer.comma();

                writer.object(true);

                writer.string("");
                writer.colon();
                writer.string("");

                writer.comma();

                writer.array(true);

                writer.string("");

                writer.comma();
                writer.string("");

                writer.array(false);
                writer.object(false);
                writer.array(false);
                writer.object(false);

            })).isEqualTo(write(

                    "{\n"+
                            "    '': '',\n"+
                            "    [\n"+
                            "        '',\n"+
                            "        {\n"+
                            "            '': '',\n"+
                            "            [\n"+
                            "                '',\n"+
                            "                ''\n"+
                            "            ]\n"+
                            "        }\n"+
                            "    ]"+
                            "\n"+
                            "}"

            ));
        }

    }

}