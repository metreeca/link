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

package com.metreeca.link.rdf4j;

import com.metreeca.link.Local;
import com.metreeca.link.rdf4j.RDF4J.Decoder;
import com.metreeca.link.rdf4j.RDF4J.Encoder;
import com.metreeca.link.rdf4j.RDF4J.Type;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Map.entry;

final class TypeLocal implements Type<Local<?>> {

    @Override public Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final Local<?> value) {
        return entry(

                value.values().entrySet().stream().flatMap(entry -> {

                    final String locale=entry.getKey().toLanguageTag();
                    final Object values=entry.getValue();

                    if ( values instanceof String ) {

                        return Stream.of(encoder.factory().createLiteral(((String)values), locale));

                    } else if ( values instanceof Collection ) {

                        return ((Collection<?>)values).stream()
                                .filter(String.class::isInstance) // expected
                                .map(v -> encoder.factory().createLiteral(((String)v), locale));

                    } else {

                        return Stream.empty(); // unexpected
                    }

                }),

                Stream.empty()
        );
    }

    @Override public Optional<Local<?>> decode(final Decoder decoder, final Value value, final Local<?> model) {
        throw new UnsupportedOperationException(";( be implemented"); // !!!
    }

}
