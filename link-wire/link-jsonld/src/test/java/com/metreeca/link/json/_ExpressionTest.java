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

import com.metreeca.link.Probe;
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.*;
import static com.metreeca.link.Shape.*;
import static com.metreeca.link.Transform.*;
import static com.metreeca.link.json._Expression.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

final class _ExpressionTest {

    private static final IRI x=iri("test:x");
    private static final IRI y=iri("test:y");
    private static final IRI z=iri("test:z");


    private static Shape test() {
        return property(x, property(y, property(z)));
    }


    @Nested
    final class Predicates {

        @Test void testDecodeIRIs() {
            assertThat(_predicate("x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .isInstanceOf(IRI.class)
                        .isEqualTo(x);

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);


            });
        }

        @Test void testDecodeEmptyProbes() {
            assertThat(_predicate("label=", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("label");
                            assertThat(probe.expression()).isEqualTo(expression());
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(x);

            });
        }

        @Test void testDecodeSingletonProbes() {
            assertThat(_predicate("label=x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("label");
                            assertThat(probe.expression()).isEqualTo(expression(x));
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);

            });
        }

        @Test void testDecodePathProbes() {
            assertThat(_predicate("label=x.y", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("label");
                            assertThat(probe.expression()).isEqualTo(expression(x, y));
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(z);

            });
        }


        @Test void testDecodeQuotedProbeLabels() {

            assertThat(_predicate("'a long label'=x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("a long label");
                            assertThat(probe.expression()).isEqualTo(expression(x));
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);

            });

            assertThat(_predicate("'x=y'=x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("x=y");
                            assertThat(probe.expression()).isEqualTo(expression(x));
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);

            });

            assertThat(_predicate("''=x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("");
                            assertThat(probe.expression()).isEqualTo(expression(x));
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);

            });

        }

        @Test void testReportMissingProbeLabels() {
            assertThatIllegalArgumentException().isThrownBy(() -> _predicate("=x", test()));
        }

    }

    @Nested
    final class Expressions {


        @Test void testDecodeEmptyPaths() {
            assertThat(_expression("", test()))
                    .isEqualTo(expression(List.of(), List.of()));
        }

        @Test void testDecodeSingletonPaths() {
            assertThat(_expression("x", test()))
                    .isEqualTo(expression(x));
        }

        @Test void testDecodeProperPaths() {
            assertThat(_expression("x.y.z", test()))
                    .isEqualTo(expression(x, y, z));
        }

        @Test void testDecodeQuotedPaths() {
            assertThat(_expression(".'a property'.'let''s escape'.''",
                    property("a property", x, property("let's escape", y, property(iri("test:"))))
            ))
                    .isEqualTo(expression(x, y, iri("test:")));
        }


        @Test void testReportUnknownFieldLabels() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> _expression("w", test()));
        }


        @Test void testDecodeTransforms() {
            assertThat(_expression("count:x", test()))
                    .isEqualTo(expression(List.of(COUNT), List.of(x)));
        }

        @Test void testDecodeTransformPipes() {
            assertThat(_expression("sum:abs:x", test()))
                    .isEqualTo(expression(List.of(SUM, ABS), List.of(x)));
        }

        @Test void testDecodeTransformPipesOnEmptyPaths() {
            assertThat(_expression("sum:abs:", test()))
                    .isEqualTo(expression(List.of(SUM, ABS), List.of()));
        }


        @Test void testReportUnknownTransforms() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> _expression("none:x", test()));
        }

        @Test void testReportMalformedExpressions() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> _expression("not an expression", test()));
        }

    }

    @Nested
    final class Queries {

        @Test void testDecodeEmptyQueries() {
            assertThat(_query("", test())).satisfies(query -> {
                assertThat(query.model().fields()).isEmpty();
                assertThat(query.filter()).isEmpty();
                assertThat(query.order()).isEmpty();
                assertThat(query.focus()).isEmpty();
            });
        }

        @Test void testDecodeEmptyPaths() {
            assertThat(_query("=value", shape()).filter().get(expression()).any())
                    .contains(Set.of(literal("value")));
        }

        @Test void testDecodeSingletonPaths() {
            assertThat(_query("x=value", property(x)).filter().get(expression(x)).any())
                    .contains(Set.of(literal("value")));
        }

        @Test void testDecodePaths() {
            assertThat(_query("x.y.z=value", test()).filter().get(expression(x, y, z)).any())
                    .contains(Set.of(literal("value")));
        }


        @Test void testReportUnknownPropertyLabels() {
            assertThatIllegalArgumentException().isThrownBy(() -> _query("w", test()));
        }

        @Test void testReportMalformedPropertyLabels() {
            assertThatIllegalArgumentException().isThrownBy(() -> _query("±w", test()));
        }


        @Test void testDecodeLtConstraints() {
            assertThat(_query("<x=value", property(x)).filter().get(expression(x)).lt())
                    .contains(literal("value"));
        }

        @Test void testDecodeGtConstraints() {
            assertThat(_query(">x=value", property(x)).filter().get(expression(x)).gt())
                    .contains(literal("value"));
        }

        @Test void testDecodeLteConstraints() {
            assertThat(_query("<%3Dx=value", property(x)).filter().get(expression(x)).lte())
                    .contains(literal("value"));
        }

        @Test void testDecodeGteConstraints() {
            assertThat(_query(">%3Dx=value", property(x)).filter().get(expression(x)).gte())
                    .contains(literal("value"));
        }

        @Test void testDecodeAlternateLteConstraints() {
            assertThat(_query("<<x=value", property(x)).filter().get(expression(x)).lte())
                    .contains(literal("value"));
        }

        @Test void testDecodeAlternateGteConstraints() {
            assertThat(_query(">>x=value", property(x)).filter().get(expression(x)).gte())
                    .contains(literal("value"));
        }


        @Test void testDecodeLikeConstraints() {
            assertThat(_query("~x=value", property(x)).filter().get(expression(x)).like())
                    .contains("value");
        }


        @Test void testDecodeSingletonAnyConstraints() {
            assertThat(_query("x=value", property(x)).filter().get(expression(x)).any())
                    .contains(Set.of(literal("value")));
        }

        @Test void testDecodeMultipleAnyConstraints() {
            assertThat(_query("x=1&x=2", property(x)).filter().get(expression(x)).any())
                    .contains(Set.of(literal("1"), literal("2")));
        }

        @Test void testDecodeNonExistentialAnyConstraints() {
            assertThat(_query("x=null", property(x)).filter().get(expression(x)).any())
                    .contains(Set.of(NIL));
        }

        @Test void testDecodeExistentialAnyConstraints() {
            assertThat(_query("x", property(x)).filter().get(expression(x)).any())
                    .contains(Set.of());
        }


        @Test void testDecodeAscendingOrder() {
            assertThat(_query("^x=1", property(x)).order().get(expression(x)))
                    .isEqualTo(1);
        }

        @Test void testDecodeDescendingOrder() {
            assertThat(_query("^x=-123", property(x)).order().get(expression(x)))
                    .isEqualTo(-123);
        }

        @Test void testDecodeAlternateIncreasingOrder() {
            assertThat(_query("^x=increasing", property(x)).order().get(expression(x)))
                    .isEqualTo(1);
        }

        @Test void testDecodeAlternateDecreasingOrder() {
            assertThat(_query("^x=decreasing", property(x)).order().get(expression(x)))
                    .isEqualTo(-1);
        }

        @Test void testReportMalformedOrder() {
            assertThatIllegalArgumentException().isThrownBy(() -> _query("^x=1.23", property(x)));
            assertThatIllegalArgumentException().isThrownBy(() -> _query("^x=value", property(x)));
        }


        @Test void testDecodeMergeMultipleConstraints() {
            assertThat(_query(">>x=lower&<<x=upper", property(x)).filter()).satisfies(filter -> {
                assertThat(filter.get(expression(x)).gte()).contains(literal("lower"));
                assertThat(filter.get(expression(x)).lte()).contains(literal("upper"));
            });
        }


        @Test void testDecodeOffset() {
            assertThat(_query("@=123", property(x)).offset())
                    .isEqualTo(123);

        }

        @Test void testReportMalformedOffset() {
            assertThatIllegalArgumentException().isThrownBy(() -> _query("@=", property(x)));
            assertThatIllegalArgumentException().isThrownBy(() -> _query("@=value", property(x)));
        }


        @Test void testDecodeLimit() {
            assertThat(_query("#=123", property(x)).offset())
                    .isEqualTo(123);

        }

        @Test void testReportMalformedLimit() {
            assertThatIllegalArgumentException().isThrownBy(() -> _query("#=", property(x)));
            assertThatIllegalArgumentException().isThrownBy(() -> _query("#=value", property(x)));
        }


        @Test void testAssignKnownDatatype() {
            assertThat(_query("<x=1", property(x, datatype(XSD.INTEGER))).filter().get(expression(x)).lt())
                    .contains(literal(integer(1)));
        }

    }

}
