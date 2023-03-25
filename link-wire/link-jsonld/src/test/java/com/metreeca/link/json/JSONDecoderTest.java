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
import com.metreeca.link.Probe;
import com.metreeca.link.Query;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Shape.*;
import static com.metreeca.link.json.JSON.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

final class JSONDecoderTest {

    private static final IRI t=iri("test:t");

    private static final IRI x=iri("test:x");
    private static final IRI y=iri("test:y");
    private static final IRI z=iri("test:z");

    private static final Literal _1=literal(integer(1));
    private static final Literal _2=literal(integer(2));
    private static final Literal _3=literal(integer(3));


    private static Map<IRI, Set<Value>> decode(final String json) { return decode(json, shape()); }

    private static Map<IRI, Set<Value>> decode(final String json, final Shape shape) {
        return json().decode(json.replace('\'', '"'), shape).fields();
    }


    private static Set<Value> value(final String value) {
        return value(value, shape());
    }

    private static Set<Value> value(final String value, final Shape shape) {
        return decode("{'value':"+value+"}", shape(
                property(RDF.VALUE, shape)
        )).getOrDefault(RDF.VALUE, Set.of());
    }

    private static Set<Value> value(final Value value) {
        return Set.of(value);
    }


    @Nested
    final class Syntax {

        @Test void testReportLocation() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("{ "))
                    .withMessageStartingWith("(1,3)")
                    .satisfies(e -> assertThat(e.getLine()).isEqualTo(1))
                    .satisfies(e -> assertThat(e.getCol()).isEqualTo(3));
        }

        @Test void testReportUnexpectedEOF() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("{"))
                    .withMessageStartingWith("(1,2)")
                    .satisfies(e -> assertThat(e.getLine()).isEqualTo(1))
                    .satisfies(e -> assertThat(e.getCol()).isEqualTo(2));

        }

        @Test void testReportTrailingGarbage() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("{} {}"))
                    .withMessageStartingWith("(1,4)")
                    .satisfies(e -> assertThat(e.getLine()).isEqualTo(1))
                    .satisfies(e -> assertThat(e.getCol()).isEqualTo(4));
        }

        @Test void testReportUnexpectedValue() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> decode("{ '@': 1 }"))
                    .withMessageStartingWith("(1,1)")
                    .satisfies(e -> assertThat(e.getLine()).isEqualTo(1))
                    .satisfies(e -> assertThat(e.getCol()).isEqualTo(1));
        }

    }

    @Nested
    final class Frames {

        @Test void testDecodeEmptyObjects() {
            assertThat(decode("{}")).isEmpty();
        }

        @Test void testDecodeSingletonObjects() {
            assertThat(decode("{'x':1}", shape(
                    property(x)
            ))).isEqualTo(Map.of(
                    x, Set.of(_1)
            ));
        }

        @Test void testDecodeFullObjects() {
            assertThat(decode("{'x':1,'y':2,'z':3}", shape(
                    property(x),
                    property(y),
                    property(z)
            ))).isEqualTo(Map.of(
                    x, Set.of(_1),
                    y, Set.of(_2),
                    z, Set.of(_3)
            ));
        }


        @Test void testIgnoreEmptyArrays() {
            assertThat(decode("{'x':[]}", shape(property(x)))).isEmpty();
        }

        @Test void testDecodeSingletonArrays() {
            assertThat(decode("{'x':[1]}", shape(
                    property(x)
            ))).isEqualTo(Map.of(
                    x, Set.of(_1)
            ));
        }

        @Test void testDecodeNullArrayItems() {
            assertThat(decode("{'x':[null]}",
                    shape(property(x))
            )).isEqualTo(Map.of(
                    x, Set.of(NIL)
            ));
        }


        @Test void testReportUnknownLabels() {
            assertThatExceptionOfType(JSONException.class).isThrownBy(() ->
                    decode("{'x':1}")
            );
        }

    }

    @Nested
    final class Resources {

        @Test void testDecodeFramedBNodes() {
            assertThat(decode("{'@id':'_:123'}", shape(
                    property(ID)
            ))).isEqualTo(Map.of(
                    ID, Set.of(bnode("123"))
            ));
        }

        @Test void testDecodeFramedIRIs() {
            assertThat(decode("{'@id':'https://example.org/'}", shape(
                    property(ID)
            ))).isEqualTo(Map.of(
                    ID, Set.of(iri("https://example.org/"))
            ));
        }

        @Test void testDecodeInlinedBNodes() {
            assertThat(value("'_:123'", shape(
                    property(x)
            ))).satisfies(values -> assertThat(values).allSatisfy(value -> assertThat(value)

                    .asInstanceOf(type(Frame.class))
                    .extracting(Frame::fields)

                    .isEqualTo(Map.of(
                            ID, Set.of(bnode("123"))
                    ))

            ));
        }

        @Test void testDecodeInlinedIRIs() {
            assertThat(value("'https://example.org/'", shape(
                    property(x)
            ))).satisfies(values -> assertThat(values).allSatisfy(value -> assertThat(value)

                    .asInstanceOf(type(Frame.class))
                    .extracting(Frame::fields)

                    .isEqualTo(Map.of(
                            ID, Set.of(iri("https://example.org/"))
                    ))

            ));
        }

    }

    @Nested
    final class Literals {

        @Test void testDecodeNulls() {
            assertThat(value("null")).isEqualTo(value(NIL));
        }

        @Test void testDecodeBooleans() {
            assertThat(value("true")).isEqualTo(value(literal(true)));
            assertThat(value("false")).isEqualTo(value(literal(false)));
        }

        @Test void testDecodeDoubles() {
            assertThat(value("-1.0234E1")).isEqualTo(value(literal(-10.234D)));
            assertThat(value("-1.0E1")).isEqualTo(value(literal(-10D)));
            assertThat(value("-1.234E0")).isEqualTo(value(literal(-1.234D)));
            assertThat(value("-0.0E0")).isEqualTo(value(literal(-0D)));
            assertThat(value("0.0E0")).isEqualTo(value(literal(0D)));
            assertThat(value("0.0e0")).isEqualTo(value(literal(0D)));
            assertThat(value("1.0E1")).isEqualTo(value(literal(+10D)));
            assertThat(value("1.234E0")).isEqualTo(value(literal(+1.234D)));
            assertThat(value("1.0234E1")).isEqualTo(value(literal(+10.234D)));
            assertThat(value("+1.0234E1")).isEqualTo(value(literal(+10.234D)));
        }

        @Test void testDecodeIntegers() {
            assertThat(value("-10")).isEqualTo(value(literal(integer(-10))));
            assertThat(value("-0")).isEqualTo(value(literal(integer(0))));
            assertThat(value("0")).isEqualTo(value(literal(integer(0))));
            assertThat(value("+0")).isEqualTo(value(literal(integer(0))));
            assertThat(value("10")).isEqualTo(value(literal(integer(10))));
            assertThat(value("+10")).isEqualTo(value(literal(integer(10))));
        }

        @Test void testDecodeDecimals() {
            assertThat(value("-10.234")).isEqualTo(value(literal(decimal(-10.234))));
            assertThat(value("-1.234")).isEqualTo(value(literal(decimal(-1.234))));
            assertThat(value("-10.0")).isEqualTo(value(literal(decimal(-10))));
            assertThat(value("-0.0")).isEqualTo(value(literal(decimal(0))));
            assertThat(value("0.0")).isEqualTo(value(literal(decimal(0))));
            assertThat(value("+0.0")).isEqualTo(value(literal(decimal(0))));
            assertThat(value("10.0")).isEqualTo(value(literal(decimal(+10))));
            assertThat(value("10.234")).isEqualTo(value(literal(decimal(+10.234))));
            assertThat(value("+10.234")).isEqualTo(value(literal(decimal(+10.234))));
        }

        @Test void testDecodeStrings() {
            assertThat(value("'value'")).isEqualTo(value(literal("value")));
        }

        @Test void testDecodeTaggeds() {
            assertThat(value(literal("value", "en")))
                    .isEqualTo(value("{'@value':'value','@language':'en'}"));
        }

        @Test void testDecodeTypeds() {
            assertThat(value("{'@value':'value','@type':'test:t'}"))
                    .isEqualTo(value(literal("value", t)));
        }


        @Test void testReportMalformedLiteral() {
            assertThatExceptionOfType(JSONException.class).isThrownBy(() -> value("{'@value':'value'}"));
            assertThatExceptionOfType(JSONException.class).isThrownBy(() -> value("{'@type':'test:t'}"));
            assertThatExceptionOfType(JSONException.class).isThrownBy(() -> value("{'@language':'en'}"));
        }

        @Test void testReportUnexpectedLiteralFields() {
            assertThatExceptionOfType(JSONException.class).isThrownBy(() ->
                    value("{'@value':'value','@type':'test:t','x':1}", property(x))
            );
        }

        @Test void testReportDuplicatedLiteralSpecialFields() {
            assertThatExceptionOfType(JSONException.class).isThrownBy(() ->
                    value("{'@value':'value','@type':'test:t','@value':''}")
            );
        }

        @Test void testReportMalformedLiteralSpecialFields() {
            assertThatExceptionOfType(JSONException.class).isThrownBy(() ->
                    value("{'@value':1,'@type':'test:t'}")
            );
        }

        @Test void testReportUnexpectedLiteralSpecialFields() {
            assertThatExceptionOfType(JSONException.class).isThrownBy(() ->
                    value("{'@value':'value','@type':'test:t','@id':1}")
            );
        }

    }

    @Nested
    final class Shorthands {

        @Test void testIncludeKnownDatatypes() {

            final BNode bnode=bnode("123");
            final IRI iri=iri("https://example.org/");

            assertThat(value("'value'", datatype(t))).isEqualTo(value(literal("value", t)));
            assertThat(value("'_:123'", datatype(BNODE))).isEqualTo(value(bnode));
            assertThat(value("'_:123'", datatype(RESOURCE))).isEqualTo(value(bnode));
            assertThat(value("'https://example.org/'", datatype(IRI))).isEqualTo(value(iri)); // !!! resolve
            assertThat(value("'https://example.org/'", datatype(RESOURCE))).isEqualTo(value(iri)); // !!! resolve

        }


        // @Test void testResolveRelativeIRIs() { }


        @Test void testDecodeEmptyLocals() {
            assertThat(value("{}", datatype(RDF.LANGSTRING)))
                    .isEmpty();
        }

        @Test void testDecodeUniqueLocals() {
            assertThat(value("{'en':'one','it':'uno'}", datatype(RDF.LANGSTRING)))
                    .containsExactly(
                            literal("one", "en"),
                            literal("uno", "it")
                    );
        }

        @Test void testDecodeCommonLocals() {
            assertThat(value("{'en':['one','two'],'it':['uno','due']}", datatype(RDF.LANGSTRING)))
                    .containsExactly(
                            literal("one", "en"),
                            literal("two", "en"),
                            literal("uno", "it"),
                            literal("due", "it")
                    );
        }

        // @Test void testDecodeRootLocal() {
        //     assertThat(value("{'':'value'}", datatype(RDF.LANGSTRING)))
        //             .isEqualTo(literal(Locale.ROOT, "value"));
        // }

        // @Test void testDecodeWildcardLocal() {
        //     assertThat(value("{'*':'value'}", datatype(RDF.LANGSTRING)))
        //             .isEqualTo(literal(Local.Wildcard, "value"));
        // }

        @Test void testReportMalformedLocals() {

            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> value("{'en':0}", datatype(RDF.LANGSTRING)));

            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> value("{'a tag':''}", datatype(RDF.LANGSTRING)));

        }

    }

    @Nested
    final class Queries {

        private Query query(final String query) {
            return value(query, property(x)).stream()
                    .filter(Query.class::isInstance)
                    .map(Query.class::cast)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("undefined query"));
        }


        @Test void testDecodePlainFields() {
            assertThat(query("{ 'x': 'value', '~x': 'keywords' }").model().fields())
                    .isEqualTo(Map.of(
                            x, Set.of(literal("value"))
                    ));
        }

        @Test void testDecodeProbeFields() {
            assertThat(query("{ 'l=x': 'value', '~x': 'keywords'  }").model().fields())
                    .allSatisfy((iri, values) -> {

                        assertThat(iri)
                                .asInstanceOf(type(Probe.class))
                                .satisfies(probe -> {

                                    assertThat(probe.label()).isEqualTo("l");
                                    assertThat(probe.expression()).isEqualTo(expression(x));
                                });

                        assertThat(values)
                                .containsExactly(literal("value"));

                    });
        }


        @Test void testDecodeLtConstraint() {
            assertThat(query("{ '<x': 'value' }").filter().get(expression(x)).lt())
                    .contains(literal("value"));
        }

        @Test void testDecodeGtConstraint() {
            assertThat(query("{ '>x': 'value' }").filter().get(expression(x)).gt())
                    .contains(literal("value"));
        }

        @Test void testDecodeLteConstraint() {
            assertThat(query("{ '<=x': 'value' }").filter().get(expression(x)).lte())
                    .contains(literal("value"));
        }

        @Test void testDecodeGteConstraint() {
            assertThat(query("{ '>=x': 'value' }").filter().get(expression(x)).gte())
                    .contains(literal("value"));
        }


        @Test void testDecodeLikeConstraint() {
            assertThat(query("{ '~x': 'value' }").filter().get(expression(x)).like())
                    .contains("value");
        }

        @Test void testReportMalformedLikeConstraint() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ '~x': 0 }"));
        }


        @Test void testDecodeAnyConstraint() {

            assertThat(query("{ '?x': [] }").filter().get(expression(x)).any())
                    .contains(Set.of());

            assertThat(query("{ '?x': [null, true, 1, 'value'] }").filter().get(expression(x)).any())
                    .contains(Set.of(NIL, literal(true), literal(integer(1)), literal("value")));

        }


        @Test void testReportUnknownConstraints() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ '±field': 0 }"));
        }


        @Test void testDecodeOrder() {
            assertThat(query("{ '^x': 1 }").order().get(expression(x)))
                    .isEqualTo(1);
        }

        @Test void testDecodeAlternateOrder() {

            assertThat(query("{ '^x': 'increasing' }").order().get(expression(x)))
                    .isEqualTo(1);

            assertThat(query("{ '^x': 'decreasing' }").order().get(expression(x)))
                    .isEqualTo(-1);

        }

        @Test void testReportMalformedOrder() {

            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ '^x': 'x' }"));

            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ '^x': 1.2 }"));

        }


        @Test void testDecodeFocus() {
            assertThat(query("{ '$x': [null] }").focus().get(expression(x)))
                    .containsExactly(NIL);
        }


        @Test void testDecodeOffset() {
            assertThat(query("{ '@': 100  }").offset())
                    .isEqualTo(100);
        }

        @Test void testReportMalformedOffset() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ '@': true  }"));
        }


        @Test void testDecodeLimit() {
            assertThat(query("{ '#': 100  }").limit())
                    .isEqualTo(100);
        }

        @Test void testReportMalformedLimit() {
            assertThatExceptionOfType(JSONException.class)
                    .isThrownBy(() -> query("{ '#': true  }"));
        }

    }

    @Nested
    final class EncodedQueries {

        @Test void testDecodeJSON() {
            assertThat(decode("{}")).isEmpty();
        }

        @Test void testDecodeURLEncodedJSON() {
            assertThat(decode("%7B%7D")).isEmpty();
        }

        @Test void testDecodeBase64JSON() {
            assertThat(decode("e30=")).isEmpty();
        }

    }

}