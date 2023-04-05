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

import com.metreeca.rest.Frame;
import com.metreeca.rest.rdf4j.RDF4J.*;

import org.eclipse.rdf4j.model.*;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Stream;

import static com.metreeca.rest.Frame.frame;

final class TypeObject implements Type<Object> {

    @Override public Entry<Stream<Value>, Stream<Statement>> encode(final Encoder encoder, final Object value) {
        return encoder.encode(frame(value));
    }

    @Override public Optional<Object> decode(final Decoder decoder, final Value value, final Object template) {
        return Optional.of(value)

                .filter(Value::isIRI)
                .map(IRI.class::cast)

                .flatMap(iri -> Optional.of(frame(template))
                        .map(frame -> frame.id() == null ? frame : frame.id(iri.stringValue()))
                        .flatMap(frame -> decoder.decode(iri, frame))
                        .map(Frame::value)
                );
    }

}
