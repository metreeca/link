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

package com.metreeca.rest.rdf4j;

import com.metreeca.rest.rdf4j.RDF4J.*;

import org.eclipse.rdf4j.model.*;

import java.net.URI;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Map.entry;

final class TypeURI implements Type<URI> {

    @Override public Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final URI value) {
        return entry(Stream.of(encoder.factory().createIRI(value.toString())), Stream.empty());
    }

    @Override public Optional<URI> decode(final Decoder decoder, final Value value, final URI template) {
        return Optional.of(value)

                .filter(Value::isIRI)
                .map(IRI.class::cast)

                .map(iri -> URI.create(iri.stringValue()));
    }

}