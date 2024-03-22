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
import com.metreeca.link.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.iri;
import static com.metreeca.link.Shape.*;
import static com.metreeca.link.Transform.*;
import static com.metreeca.link.json._Parser.expression;
import static com.metreeca.link.json._Parser.predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

final class _ParserTest {

    private static final IRI x=iri("test:x");
    private static final IRI y=iri("test:y");
    private static final IRI z=iri("test:z");


    private static Shape test() {
        return property(x, property(y, property(z, shape())));
    }


    @Nested
    final class Predicates {

        @Test void testDecodeIRIs() {
            assertThat(predicate("x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .isInstanceOf(IRI.class)
                        .isEqualTo(x);

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);


            });
        }

        @Test void testDecodeEmptyProbes() {
            assertThat(predicate("label=", test())).satisfies(e -> {

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
            assertThat(predicate("label=x", test())).satisfies(e -> {

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
            assertThat(predicate("label=x.y", test())).satisfies(e -> {

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

            assertThat(predicate("'a long label'=x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("a long label");
                            assertThat(probe.expression()).isEqualTo(expression(x));
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);

            });

            assertThat(predicate("'x=y'=x", test())).satisfies(e -> {

                assertThat(e.getKey())
                        .asInstanceOf(type(Probe.class))
                        .satisfies(probe -> {
                            assertThat(probe.label()).isEqualTo("x=y");
                            assertThat(probe.expression()).isEqualTo(expression(x));
                        });

                assertThat(e.getValue().predicates())
                        .containsOnlyKeys(y);

            });

            assertThat(predicate("''=x", test())).satisfies(e -> {

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
            assertThatIllegalArgumentException().isThrownBy(() -> predicate("=x", test()));
        }

    }

    @Nested
    final class Expressions {

        @Test void testDecodeEmptyPaths() {
            assertThat(expression("", test()))
                    .isEqualTo(expression(List.of(), List.of()));
        }

        @Test void testDecodeSingletonPaths() {
            assertThat(expression("x", test()))
                    .isEqualTo(expression(x));
        }

        @Test void testDecodeProperPaths() {
            assertThat(expression("x.y.z", test()))
                    .isEqualTo(expression(x, y, z));
        }

        @Test void testDecodeQuotedPaths() {
            assertThat(expression(".'a property'.'let''s escape'.''",
                    property("a property", x, property("let's escape", y, property(iri("test:"), shape())))
            ))
                    .isEqualTo(expression(x, y, iri("test:")));
        }


        @Test void testReportUnknownFieldLabels() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> expression("w", test()));
        }


        @Test void testDecodeTransforms() {
            assertThat(expression("count:x", test()))
                    .isEqualTo(expression(List.of(COUNT), List.of(x)));
        }

        @Test void testDecodeTransformPipes() {
            assertThat(expression("sum:abs:x", test()))
                    .isEqualTo(expression(List.of(SUM, ABS), List.of(x)));
        }

        @Test void testDecodeTransformPipesOnEmptyPaths() {
            assertThat(expression("sum:abs:", test()))
                    .isEqualTo(expression(List.of(SUM, ABS), List.of()));
        }


        @Test void testReportUnknownTransforms() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> expression("none:x", test()));
        }

        @Test void testReportMalformedExpressions() {
            assertThatIllegalArgumentException()
                    .isThrownBy(() -> expression("not an expression", test()));
        }

    }

    @Nested
    final class Values {

        @Test void testParseEmptyIRISAsNil() {
            assertThat(_Parser.value("", id(), URI.create("https://example.net/")))
                    .isEqualTo(Frame.NIL);
        }

    }

}
