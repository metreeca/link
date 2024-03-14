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

import com.metreeca.link.Frame;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.List;

import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.decimal;
import static com.metreeca.link.Frame.integer;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Probe.probe;
import static com.metreeca.link.Shape.*;
import static com.metreeca.link.Transform.ABS;
import static com.metreeca.link.json.JSON.json;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

final class JSONEncoderTest {

    private static final URI base=URI.create("https://example.org/base/");

    private static final IRI t=iri("test:t");
    private static final IRI x=iri("test:x");
    private static final IRI y=iri("test:y");


    private static String encode(final Shape shape, final Frame frame) {
        try {

            final StringWriter writer=new StringWriter();

            json().encode(base, writer, shape, frame);

            return writer.toString();

        } catch ( final IOException e ) {

            throw new UncheckedIOException(e);

        }
    }

    private static String encode(final String string) {
        return string
                .replace('\'', '"')
                .replace("\n", "");
    }


    @Nested
    final class Frames {

        @Test void testHandleEmptyFrame() {
            assertThat(encode(shape(), frame())).isEqualTo(encode(

                    "{}"

            ));
        }

        @Test void testHandleIds() {
            assertThat(encode(shape(

                    property(ID)

            ), frame(

                    field(ID, x)

            ))).isEqualTo(encode(

                    "{'@id':'test:x'}"

            ));
        }

        @Test void testHandleAliasedIds() {
            assertThat(encode(shape(

                    property("id", ID)

            ), frame(

                    field(ID, x)

            ))).isEqualTo(encode(

                    "{'id':'test:x'}"

            ));
        }

        @Test void testHandleTypes() {
            assertThat(encode(shape(

                    property(RDF.TYPE)

            ), frame(

                    field(RDF.TYPE, x)

            ))).isEqualTo(encode(

                    "{'@type':['test:x']}"

            ));
        }

        @Test void testHandleAliasedTypes() {
            assertThat(encode(shape(

                    property("type", RDF.TYPE)

            ), frame(

                    field(RDF.TYPE, x)

            ))).isEqualTo(encode(

                    "{'type':['test:x']}"

            ));
        }

        @Test void testHandlePredicates() {
            assertThat(encode(shape(

                    property(x),
                    property(y)

            ), frame(

                    field(x, literal("one")),
                    field(y, literal("two"))

            ))).isEqualTo(encode(

                    "{'x':['one'],'y':['two']}"

            ));
        }

        @Test void testIgnoreUnknownPredicates() {
            assertThat(encode(shape(

                    property(x)

            ), frame(

                    field(x, literal("one")),
                    field(y, literal("two"))

            ))).isEqualTo(encode(

                    "{'x':['one']}"

            ));
        }

        @Test void testHandleProbes() {
            assertThat(encode(shape(), frame(

                    field(probe("x", expression()), literal("one")),
                    field(probe("y", expression()), literal("two"))

            ))).isEqualTo(encode(

                    "{'x':'one','y':'two'}"

            ));

        }

        @Test void testHandleNestedFrames() {
            assertThat(encode(shape(

                    property(x)

            ), frame(

                    field(x, frame(), frame())

            ))).isEqualTo(encode(

                    "{'x':[{},{}]}"

            ));
        }

    }

    @Nested
    final class Resources {

        @Test void testHandleBNodesAsFrames() {
            assertThat(encode(

                    shape(property(RDF.VALUE)),
                    frame(field(RDF.VALUE, bnode("x")))

            )).isEqualTo(encode(

                    "{'value':[{'@id':'_:x'}]}"

            ));
        }

        @Test void testHandleIRIsAsFrames() {
            assertThat(encode(

                    shape(property(RDF.VALUE)),
                    frame(field(RDF.VALUE, iri("test:x")))

            )).isEqualTo(encode(

                    "{'value':[{'@id':'test:x'}]}"

            ));
        }

    }

    @Nested
    final class Literals {

        private String value(final Value value) {
            return encode(shape(property(RDF.VALUE)), frame(field(RDF.VALUE, value)));
        }

        private String value(final String string) {
            return encode(format("{'value':[%s]}", string));
        }


        @Test void testEncodeNulls() {
            assertThat(value(NIL)).isEqualTo(value("null"));
        }

        @Test void testEncodeBooleans() {
            assertThat(value(literal(true))).isEqualTo(value("true"));
            assertThat(value(literal(false))).isEqualTo(value("false"));
        }

        @Test void testEncodeDoubles() {
            assertThat(value(literal(-10.234D))).isEqualTo(value("-1.0234E1"));
            assertThat(value(literal(-10D))).isEqualTo(value("-1.0E1"));
            assertThat(value(literal(-1.234D))).isEqualTo(value("-1.234E0"));
            assertThat(value(literal(-0D))).isEqualTo(value("-0.0E0"));
            assertThat(value(literal(0D))).isEqualTo(value("0.0E0"));
            assertThat(value(literal(+10D))).isEqualTo(value("1.0E1"));
            assertThat(value(literal(+1.234D))).isEqualTo(value("1.234E0"));
            assertThat(value(literal(+10.234D))).isEqualTo(value("1.0234E1"));
        }

        @Test void testEncodeIntegers() {
            assertThat(value(literal(integer(-10)))).isEqualTo(value("-10"));
            assertThat(value(literal(integer(0)))).isEqualTo(value("0"));
            assertThat(value(literal(integer(+10)))).isEqualTo(value("10"));
        }

        @Test void testEncodeDecimals() {
            assertThat(value(literal(decimal(-10.234)))).isEqualTo(value("-10.234"));
            assertThat(value(literal(decimal(-1.234)))).isEqualTo(value("-1.234"));
            assertThat(value(literal(decimal(-10)))).isEqualTo(value("-10.0"));
            assertThat(value(literal(decimal(0)))).isEqualTo(value("0.0"));
            assertThat(value(literal(decimal(+10)))).isEqualTo(value("10.0"));
            assertThat(value(literal(decimal(+10.234)))).isEqualTo(value("10.234"));
        }

        @Test void testEncodeNumerics() {

            assertThat(value(literal(1)))
                    .isEqualTo(value(format("{'@value':'1','@type':'%s'}", XSD.INT)));

            assertThat(value(literal(1.0F)))
                    .isEqualTo(value(format("{'@value':'1.0','@type':'%s'}", XSD.FLOAT)));

        }

        @Test void testEncodeStrings() {
            assertThat(value(literal("value"))).isEqualTo(value("'value'"));
        }

        @Test void testEncodeTaggeds() {
            assertThat(value(literal("value", "en")))
                    .isEqualTo(value("{'@value':'value','@language':'en'}"));
        }

        @Test void testEncodeTypeds() {
            assertThat(value(literal("value", t)))
                    .isEqualTo(value("{'@value':'value','@type':'test:t'}"));
        }

    }

    @Nested
    final class Shorthands {

        private String value(final Shape shape, final Value... values) {
            return encode(shape(property(RDF.VALUE, shape)), frame(field(RDF.VALUE, values)));
        }

        private String value(final String string) {
            return encode(format("{'value':%s}", string));
        }


        @Test void testOmitArrayOnScalarProperties() {
            assertThat(value(maxCount(1), literal("value"))).isEqualTo(value("'value'"));
        }

        @Test void testOmitKnownDatatypes() {
            assertThat(value(datatype(t), literal("value", t))).isEqualTo(value("['value']"));
        }

        @Test void testUseRootRelativeIRIs() {
            assertThat(value(datatype(IRI), iri(base.resolve("/path/name")))).isEqualTo(value("['/path/name']"));
        }

        @Test void testCompactKnownTaggedLiterals() {
            assertThat(value(
                    shape(datatype(RDF.LANGSTRING)),
                    literal("one", "en"),
                    literal("two", "en"),
                    literal("uno", "it"),
                    literal("due", "it")
            )).isEqualTo(value("{'en':['one','two'],'it':['uno','due']}"));
        }

        @Test void testCompactKnownUniqueTaggedLiterals() {
            assertThat(value(
                    shape(datatype(RDF.LANGSTRING), maxCount(1)),
                    literal("one", "en"),
                    literal("uno", "it")
            )).isEqualTo(value("{'en':'one','it':'uno'}"));
        }


        @Test void testOmitArrayOnTabularFrames() {
            assertThat(encode(shape(

                    property(x)

            ), frame(

                    field(probe("a", expression(List.of(ABS), List.of())), literal(integer(1))),
                    field(probe("b", expression(x)), literal("v"))

            ))).isEqualTo(encode(

                    "{'a':1,'b':'v'}"

            ));
        }

    }

}