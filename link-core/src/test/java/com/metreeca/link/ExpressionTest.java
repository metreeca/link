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

package com.metreeca.link;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.metreeca.link.Expression.expression;
import static com.metreeca.link.Frame.iri;
import static com.metreeca.link.Shape.datatype;
import static com.metreeca.link.Shape.property;
import static com.metreeca.link.Transform.*;

import static org.assertj.core.api.Assertions.assertThat;

final class ExpressionTest {

    private static final IRI x=iri("test:x");
    private static final IRI y=iri("test:y");


    @Nested
    final class Shapes {

        @Test void testTraverseEmptyPath() {

            final Expression expression=expression();
            final Shape shape=datatype(XSD.INTEGER);

            assertThat(expression.apply(shape).datatype())
                    .contains(XSD.INTEGER);
        }

        @Test void testTraverseSingletonPath() {

            final Expression expression=expression(x);
            final Shape shape=property(x, datatype(XSD.INTEGER));

            assertThat(expression.apply(shape).datatype())
                    .contains(XSD.INTEGER);
        }

        @Test void testTraversePath() {

            final Expression expression=expression(x, y);
            final Shape shape=property(x, property(y, datatype(XSD.INTEGER)));

            assertThat(expression.apply(shape).datatype())
                    .contains(XSD.INTEGER);
        }


        @Test void testApplySingletonTransformPipe() {

            final Expression expression=expression(List.of(MIN), List.of(RDF.VALUE));
            final Shape shape=property(RDF.VALUE, datatype(XSD.INTEGER));

            assertThat(expression.apply(shape).datatype())
                    .contains(XSD.INTEGER);

        }

        @Test void testApplyTransformPipe() {

            final Expression expression=expression(List.of(AVG, ROUND), List.of(RDF.VALUE));
            final Shape shape=property(RDF.VALUE, datatype(XSD.INTEGER));

            assertThat(expression.apply(shape).datatype())
                    .contains(XSD.DECIMAL);

        }

    }

}