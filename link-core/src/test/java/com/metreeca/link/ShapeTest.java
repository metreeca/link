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

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import static com.metreeca.link.Frame.field;
import static com.metreeca.link.Frame.frame;
import static com.metreeca.link.Shape.property;

import static org.assertj.core.api.Assertions.assertThat;

final class ShapeTest {

    private static final Shape recursive=property(RDF.VALUE, new Supplier<>() {

        @Override public Shape get() { return recursive; }

    });


    @Test void testHandleRecursiveDefinitions() {
        assertThat(recursive.entry(RDF.VALUE).map(Entry::getValue))
                .contains(recursive);
    }


    @Nested
    final class Validation {

        private Set<String> errors(final Shape shape, final Value... values) {
            return property(RDF.VALUE, shape)
                    .validate(frame(field(RDF.VALUE, values)))
                    .map(trace -> trace.entries().get(RDF.VALUE))
                    .map(Trace::errors)
                    .orElseGet(Set::of);
        }


        // @Test void test() {
        //
        //     final IRI x=iri("test:x");
        //     final IRI y=iri("test:y");
        //
        //     assertThat(errors(clazz(x), frame(field(RDF.TYPE, y)))).isEmpty();
        //     assertThat(errors(clazz(x), frame())).as("missing rdf:type").isNotEmpty();
        //     assertThat(errors(clazz(x), literal(0))).as("not a resource").isNotEmpty();
        //
        // }

    }

}